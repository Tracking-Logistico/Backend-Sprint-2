package com.udea.demo.pedidos.application.service;

import com.udea.demo.pedidos.application.dto.*;
import com.udea.demo.pedidos.domain.exception.PedidoNoEncontradoException;
import com.udea.demo.pedidos.domain.model.*;
import com.udea.demo.pedidos.interfaces.persistence.HistorialPedidoRepository;
import com.udea.demo.pedidos.interfaces.persistence.PedidoRepository;
import com.udea.demo.usuarios.application.service.ActorAuthorizationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConsultaPedidoUseCase {
    private final PedidoRepository pedidos;
    private final HistorialPedidoRepository historial;
    private final ActorAuthorizationService actores;
    private final SeguimientoClienteService seguimientoCliente;
    private final AutorizacionLecturaPedido autorizacion;
    private final PedidoMapeador mapeador;

    public ConsultaPedidoUseCase(PedidoRepository pedidos, HistorialPedidoRepository historial,
            ActorAuthorizationService actores, SeguimientoClienteService seguimientoCliente,
            AutorizacionLecturaPedido autorizacion, PedidoMapeador mapeador) {
        this.pedidos = pedidos;
        this.historial = historial;
        this.actores = actores;
        this.seguimientoCliente = seguimientoCliente;
        this.autorizacion = autorizacion;
        this.mapeador = mapeador;
    }

    @Transactional(readOnly = true)
    public Page<PedidoClienteResponseDTO> listarMios(EstadoPedido estado, LocalDateTime fechaDesde,
                                                      LocalDateTime fechaHasta, Pageable pageable) {
        var actor = actores.actorActual();
        List<EstadoPedido> estadosNoAsociados = List.of(
                EstadoPedido.SOLICITADO, EstadoPedido.CORRECCION_SOLICITADA, EstadoPedido.RECHAZADO);
        return pedidos.findPedidosDeCliente(actores.clienteActualId(), actor.getEmail(),
                        estadosNoAsociados, estado, fechaDesde, fechaHasta, pageable)
                .map(mapeador::mapCliente);
    }

    @Transactional(readOnly = true)
    public Page<PedidoResponseDTO> listarPendientes(Pageable pageable) {
        return pedidos.findPendientes(EstadoPedido.SOLICITADO, EstadoPedido.CORRECCION_SOLICITADA, pageable)
                .map(mapeador::map);
    }

    @Transactional(readOnly = true)
    public Page<PedidoResponseDTO> listarValidados(Pageable pageable) {
        return pedidos.findByEstadoAndFechaValidacionIsNotNullOrderByFechaCreacionAsc(
                EstadoPedido.SOLICITADO, pageable).map(mapeador::map);
    }

    @Transactional(readOnly = true)
    public Page<PedidoResponseDTO> listarActivables(Pageable pageable) {
        return pedidos.findActivables(EstadoPedido.SOLICITADO, EstadoPedido.CREADO, pageable)
                .map(mapeador::map);
    }

    @Transactional(readOnly = true)
    public List<PedidoResponseDTO> listarDespachos() {
        return pedidos.findByEstadoInOrderByFechaCreacionAsc(List.of(
                EstadoPedido.SOLICITADO, EstadoPedido.CREADO,
                EstadoPedido.RECIBIDO_EN_ORIGEN, EstadoPedido.EN_TRANSITO)).stream()
                .filter(p -> p.getEstado() != EstadoPedido.SOLICITADO || p.getFechaValidacion() != null)
                .map(mapeador::map).toList();
    }

    @Transactional(readOnly = true)
    public Page<PedidoResponseDTO> listarEnTransito(Pageable pageable) {
        return pedidos.findByEstadoInOrderByFechaCreacionAsc(
                List.of(EstadoPedido.RECIBIDO_EN_ORIGEN, EstadoPedido.EN_TRANSITO), pageable)
                .map(mapeador::map);
    }

    @Transactional(readOnly = true)
    public PedidoResponseDTO obtener(Long id) {
        Pedido p = buscar(id); autorizacion.autorizarLectura(p); return mapeador.map(p);
    }

    @Transactional(readOnly = true)
    public PedidoResponseDTO obtenerPorTracking(String numeroTracking) {
        Pedido p = pedidos.findByNumeroTracking(numeroTracking).orElseThrow(() -> new PedidoNoEncontradoException(numeroTracking));
        autorizacion.autorizarLectura(p); return mapeador.map(p);
    }

    @Transactional(readOnly = true)
    public SeguimientoClienteResponseDTO obtenerSeguimientoCliente(String numeroTracking) {
        return seguimientoCliente.obtener(numeroTracking);
    }

    @Transactional(readOnly = true)
    public List<HistorialPedidoResponseDTO> historial(Long id) {
        Pedido p = buscar(id); autorizacion.autorizarLectura(p);
        return historial.findByPedidoIdOrderByFechaAsc(id).stream()
                .map(h -> new HistorialPedidoResponseDTO(h.getId(), h.getUsuarioId(), h.getTipoEvento(),
                        h.getCampoObservado(), h.getDetalle(), h.getFecha())).toList();
    }

    @Transactional(readOnly = true)
    public java.util.Map<Long, PedidoResponseDTO> obtenerPorIds(java.util.Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return java.util.Map.of();
        return pedidos.findByIdIn(ids).stream()
                .collect(java.util.stream.Collectors.toMap(Pedido::getId, mapeador::map));
    }

    @Transactional(readOnly = true)
    public PedidoResponseDTO obtenerParaConductor(Long pedidoId) {
        Pedido p = buscar(pedidoId);
        autorizacion.autorizarConductor(p);
        return mapeador.map(p);
    }

    @Transactional(readOnly = true)
    public List<HistorialPedidoResponseDTO> historialParaConductor(Long id) {
        Pedido p = buscar(id);
        autorizacion.autorizarHistorialConductor(p);
        return historial.findByPedidoIdOrderByFechaAsc(id).stream()
                .map(h -> new HistorialPedidoResponseDTO(h.getId(), h.getUsuarioId(), h.getTipoEvento(),
                        h.getCampoObservado(), h.getDetalle(), h.getFecha())).toList();
    }

    private Pedido buscar(Long id) { return pedidos.findById(id).orElseThrow(() -> new PedidoNoEncontradoException(id)); }
}
