package com.udea.demo.rutas.application.service;

import com.udea.demo.pedidos.application.dto.CambiarEstadoLogisticoRequestDTO;
import com.udea.demo.pedidos.application.dto.PedidoResponseDTO;
import com.udea.demo.pedidos.domain.model.EstadoPedido;
import com.udea.demo.rutas.interfaces.services.EnviosParaRutaPort;
import com.udea.demo.rutas.application.dto.*;
import com.udea.demo.rutas.domain.exception.*;
import com.udea.demo.rutas.domain.model.*;
import com.udea.demo.rutas.interfaces.persistence.*;
import com.udea.demo.rutas.interfaces.services.RutaServiceI;
import com.udea.demo.usuarios.domain.model.Conductor;
import com.udea.demo.usuarios.domain.model.Rol;
import com.udea.demo.usuarios.interfaces.persistence.ConductorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Comparator;
import java.util.List;
import java.util.HashSet;

@Service
public class RutaService implements RutaServiceI {
    private final RutaRepository rutas;
    private final ParadaRutaRepository paradas;
    private final GestorRutaActiva gestor;
    private final EnviosParaRutaPort pedidos;
    private final RutaMapeador mapeador;
    private final ValidadorCapacidadRuta capacidad;
    private final ConductorRepository conductores;
    private final RegistroAsignacionService registro;
    @Value("${app.operations.time-zone:America/Bogota}")
    private String zonaHoraria = "UTC";

    public RutaService(RutaRepository rutas, ParadaRutaRepository paradas, GestorRutaActiva gestor,
                       EnviosParaRutaPort pedidos, ConductorRepository conductores, RegistroAsignacionService registro,
                       RutaMapeador mapeador, ValidadorCapacidadRuta capacidad) {
        this.rutas = rutas; this.paradas = paradas; this.gestor = gestor; this.pedidos = pedidos; this.conductores = conductores; this.registro = registro;
        this.mapeador = mapeador; this.capacidad = capacidad;
    }

    @Override @Transactional(readOnly = true)
    public Page<PedidoResponseDTO> listarEnviosPendientesDeAsignacion(Pageable pageable) {
        java.util.Set<Long> asignados = new java.util.HashSet<>(paradas.findPedidoIdsByEstado(EstadoParada.PENDIENTE));
        Page<PedidoResponseDTO> pagina = pedidos.listarEnTransito(pageable);
        List<PedidoResponseDTO> contenido = pagina.getContent().stream()
                .filter(p -> !asignados.contains(p.id())).toList();
        return new org.springframework.data.domain.PageImpl<>(contenido, pageable, pagina.getTotalElements());
    }

    @Override @Transactional(readOnly = true)
    public List<ConductorDisponibleDTO> listarConductoresDisponibles() {
        return conductores.findAll().stream()
                .filter(c -> c.getUsuario() != null && Boolean.TRUE.equals(c.getUsuario().getActivo())
                        && "ACTIVO".equalsIgnoreCase(c.getEstado())
                        && c.getUsuario().getRol() == Rol.CONDUCTOR)
                .map(c -> {
                    List<PedidoResponseDTO> asignados = rutas.findByConductorIdAndFecha(c.getId(), LocalDate.now(ZoneId.of(zonaHoraria)))
                            .map(r -> r.getParadas().stream().filter(p -> p.getEstado() == EstadoParada.PENDIENTE)
                                .map(p -> pedidos.obtener(p.getPedidoId())).toList())
                            .orElseGet(List::of);
                    return new ConductorDisponibleDTO(c.getUsuario().getId(), c.getUsuario().getNombre(), c.getEstado(),
                        c.getCapacidadMaxKg(), c.getCapacidadMaxVolumenCm3(), c.getMaxEntregasDia(),
                        asignados.size(), asignados.stream().mapToDouble(PedidoResponseDTO::pesoKg).sum(),
                        asignados.stream().mapToDouble(this::volumen).sum());
                })
                .sorted(Comparator.comparing(ConductorDisponibleDTO::nombre))
                .toList();
    }

    @Override @Transactional
    public RutaResponseDTO asignarEnvio(AsignarEnvioRequestDTO dto) {
        paradas.findByPedidoIdAndEstado(dto.pedidoId(), EstadoParada.PENDIENTE)
            .ifPresent(p -> { throw new EnvioYaAsignadoException(dto.pedidoId()); });
        return asignarVarios(new AsignacionMasivaRequestDTO(dto.conductorId(), List.of(dto.pedidoId())));
    }

    @Override @Transactional
    public RutaResponseDTO asignarVarios(AsignacionMasivaRequestDTO dto) {
        if (dto.pedidoIds() == null || dto.pedidoIds().isEmpty() || dto.pedidoIds().size() > 100 ||
            dto.pedidoIds().stream().anyMatch(java.util.Objects::isNull) || dto.pedidoIds().stream().anyMatch(id -> id <= 0) ||
            new HashSet<>(dto.pedidoIds()).size() != dto.pedidoIds().size())
            throw new IllegalArgumentException("Selecciona entre 1 y 100 pedidos distintos");
        Conductor conductor = conductorPorUsuario(dto.conductorId(), true);
        Ruta ruta = gestor.obtenerOCrear(conductor.getId());
        for (Long pedidoId : dto.pedidoIds()) {
            paradas.findByPedidoIdAndEstado(pedidoId, EstadoParada.PENDIENTE)
                .ifPresent(p -> { throw new EnvioYaAsignadoException(pedidoId); });
            PedidoResponseDTO pedido = pedidos.obtener(pedidoId);
            if (pedido.estado() != EstadoPedido.RECIBIDO_EN_ORIGEN && pedido.estado() != EstadoPedido.EN_TRANSITO)
                throw new IllegalStateException("Solo pueden asignarse envíos recibidos en origen o en tránsito");
            validarCapacidad(ruta, conductor, pedido);
            ruta.agregarParada(pedidoId);
            pedidos.cambiarEstadoLogistico(pedidoId, new CambiarEstadoLogisticoRequestDTO(EstadoPedido.EN_REPARTO));
            registro.registrar(pedidoId, null, dto.conductorId(), "ASIGNACION", null);
        }
        priorizarAltas(ruta);
        return mapeador.map(rutas.saveAndFlush(ruta));
    }

    @Override @Transactional
    public RutaResponseDTO reordenarRuta(Long rutaId, ReordenarRutaRequestDTO dto) {
        Ruta ruta = rutas.findById(rutaId).orElseThrow(() -> RutaNoEncontradaException.porId(rutaId));
        ruta.reordenar(dto.pedidoIdsEnOrden());
        return mapeador.map(rutas.save(ruta));
    }

    @Override @Transactional
    public RutaResponseDTO reasignarEnvio(ReasignarEnvioRequestDTO dto) {
        ParadaRuta actual = paradas.findByPedidoIdAndEstado(dto.pedidoId(), EstadoParada.PENDIENTE)
                .orElseThrow(() -> new EnvioNoAsignadoException(dto.pedidoId()));
        Long anteriorId = conductores.findById(actual.getRuta().getConductorId())
                .map(c -> c.getUsuario().getId()).orElse(actual.getRuta().getConductorId());
        if (anteriorId.equals(dto.nuevoConductorId()))
            throw new IllegalArgumentException("El nuevo conductor debe ser diferente del actual");
        Conductor nuevo = conductorPorUsuario(dto.nuevoConductorId(), true);
        Ruta destino = gestor.obtenerOCrear(nuevo.getId());
        PedidoResponseDTO pedido = pedidos.obtener(dto.pedidoId());
        if (pedido.estado() != EstadoPedido.EN_REPARTO)
            throw new IllegalStateException("Solo se pueden reasignar envíos en reparto");
        validarCapacidad(destino, nuevo, pedido);
        actual.getRuta().cancelarParada(dto.pedidoId());

        rutas.saveAndFlush(actual.getRuta());
        destino.agregarParada(dto.pedidoId());
        priorizarAltas(destino);
        registro.registrar(dto.pedidoId(), anteriorId, dto.nuevoConductorId(), "REASIGNACION", dto.motivo());
        return mapeador.map(rutas.saveAndFlush(destino));
    }

    @Override @Transactional(readOnly = true)
    public RutaResponseDTO obtenerRutaActivaDeConductor(Long conductorUsuarioId) {
        Conductor conductor = conductorPorUsuario(conductorUsuarioId);
        return rutaActivaDeConductor(conductor, conductorUsuarioId);
    }

    private RutaResponseDTO rutaActivaDeConductor(Conductor conductor, Long conductorUsuarioId) {
        Ruta ruta = rutas.findByConductorIdAndFecha(conductor.getId(), LocalDate.now(ZoneId.of(zonaHoraria)))
                .orElseThrow(() -> RutaNoEncontradaException.paraConductor(conductorUsuarioId));
        return mapeador.map(ruta);
    }

    private void priorizarAltas(Ruta ruta) {
        List<Long> orden = ruta.getParadas().stream()
                .filter(p -> p.getEstado() == EstadoParada.PENDIENTE)
                .sorted(Comparator.comparingInt(p -> prioridadOrden(pedidos.obtener(p.getPedidoId()))))
                .map(ParadaRuta::getPedidoId).toList();
        if (!orden.isEmpty() && !ruta.isOrdenManual()) ruta.reordenarAutomatico(orden);
    }

    private int prioridadOrden(PedidoResponseDTO p) {
        var prioridad = p.prioridadConfirmada() != null ? p.prioridadConfirmada() : p.prioridadSugerida();
        return switch (prioridad) {
            case ALTA -> 0;
            case MEDIA -> 1;
            case BAJA -> 2;
        };
    }

    

    

    private Conductor conductorPorUsuario(Long usuarioId) { return conductorPorUsuario(usuarioId, false); }

    private Conductor conductorPorUsuario(Long usuarioId, boolean bloquear) {
        return (bloquear ? conductores.findByUsuarioIdForUpdate(usuarioId) : conductores.findByUsuarioId(usuarioId))
                .filter(c -> c.getUsuario() != null && Boolean.TRUE.equals(c.getUsuario().getActivo())
                        && c.getUsuario().getRol() == Rol.CONDUCTOR
                        && "ACTIVO".equalsIgnoreCase(c.getEstado()))
                .orElseThrow(() -> new IllegalArgumentException("No existe un conductor activo para el usuario indicado"));
    }

    
    private void validarCapacidad(Ruta ruta, Conductor conductor, PedidoResponseDTO nuevo) {
        List<PedidoResponseDTO> actuales = ruta.getParadas().stream()
                .filter(parada -> parada.getEstado() == EstadoParada.PENDIENTE)
                .map(parada -> pedidos.obtener(parada.getPedidoId())).toList();
        capacidad.validar(conductor, actuales, nuevo);
    }

    private double volumen(PedidoResponseDTO pedido) {
        return pedido.largoCm() * pedido.anchoCm() * pedido.altoCm();
    }

}
