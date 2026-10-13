package com.udea.demo.pedidos.application.service;

import com.udea.demo.pedidos.application.dto.*;
import com.udea.demo.pedidos.domain.exception.PedidoNoEncontradoException;
import com.udea.demo.pedidos.domain.model.*;
import com.udea.demo.pedidos.domain.service.GeneradorEtiqueta;
import com.udea.demo.pedidos.interfaces.persistence.PedidoRepository;
import com.udea.demo.usuarios.application.service.ActorAuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class OperacionLogisticaPedidoUseCase {
    private final PedidoRepository pedidos;
    private final ActorAuthorizationService actores;
    private final GeneradorNumeroTracking numerosTracking;
    private final GeneradorEtiqueta etiquetas;
    private final RegistroHistorialPedido historial;
    private final PedidoMapeador mapeador;

    public OperacionLogisticaPedidoUseCase(PedidoRepository pedidos, ActorAuthorizationService actores,
            GeneradorNumeroTracking numerosTracking, GeneradorEtiqueta etiquetas,
            RegistroHistorialPedido historial, PedidoMapeador mapeador) {
        this.pedidos = pedidos;
        this.actores = actores;
        this.numerosTracking = numerosTracking;
        this.etiquetas = etiquetas;
        this.historial = historial;
        this.mapeador = mapeador;
    }

    @Transactional
    public PedidoResponseDTO activarTracking(Long id) {
        actores.operadorActualId();
        Pedido p = pedidos.findByIdForUpdate(id).orElseThrow(() -> new PedidoNoEncontradoException(id));
        if (p.getNumeroTracking() != null) return mapeador.map(p);
        p.activarTracking(generarTrackingUnico());
        Pedido guardado = pedidos.save(p);
        evento(id, actores.actorActual().getId(), "TRACKING_ACTIVADO", null, guardado.getNumeroTracking());
        return mapeador.map(guardado);
    }

    @Transactional
    public PedidoResponseDTO cambiarEstadoLogistico(Long id, CambiarEstadoLogisticoRequestDTO dto) {
        actores.operadorActualId();
        Pedido p = buscar(id);
        p.cambiarEstadoLogistico(dto.estado());
        evento(id, actores.actorActual().getId(), "ESTADO_LOGISTICO", null, dto.estado().name());
        return mapeador.map(pedidos.save(p));
    }

    @Transactional
    public EtiquetaEnvioResponseDTO generarEtiqueta(Long id) {
        actores.operadorActualId();
        Pedido p = pedidos.findByIdForUpdate(id).orElseThrow(() -> new PedidoNoEncontradoException(id));
        if (p.getNumeroTracking() == null) throw new com.udea.demo.pedidos.domain.exception.TrackingNoActivoException(id);

        String contenido = etiquetas.generar(p);
        p.confirmarImpresionEtiqueta();
        pedidos.save(p);
        evento(id, actores.actorActual().getId(), "ETIQUETA_GENERADA", null, null);
        return new EtiquetaEnvioResponseDTO(p.getNumeroPedido(), p.getNumeroTracking(), contenido, p.getFechaImpresionEtiqueta());
    }

    private String generarTrackingUnico() {
        for (int i = 0; i < 10; i++) {
            String tracking = numerosTracking.generar();
            if (!pedidos.existsByNumeroTracking(tracking)) return tracking;
        }
        throw new IllegalStateException("No fue posible generar un código de tracking único");
    }

    private Pedido buscar(Long id) { return pedidos.findById(id).orElseThrow(() -> new PedidoNoEncontradoException(id)); }

    private void evento(Long pedidoId, Long usuarioId, String tipo, String campo, String detalle) {
        historial.registrar(pedidoId, usuarioId, tipo, campo, detalle, LocalDateTime.now());
    }
}
