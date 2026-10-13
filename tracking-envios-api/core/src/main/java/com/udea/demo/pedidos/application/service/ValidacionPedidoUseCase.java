package com.udea.demo.pedidos.application.service;

import com.udea.demo.pedidos.application.dto.*;
import com.udea.demo.pedidos.domain.exception.PedidoNoEncontradoException;
import com.udea.demo.pedidos.domain.model.*;
import com.udea.demo.pedidos.interfaces.persistence.PedidoRepository;
import com.udea.demo.usuarios.application.service.ActorAuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class ValidacionPedidoUseCase {
    private final PedidoRepository pedidos;
    private final ActorAuthorizationService actores;
    private final RegistroHistorialPedido historial;
    private final PedidoMapeador mapeador;

    public ValidacionPedidoUseCase(PedidoRepository pedidos, ActorAuthorizationService actores,
            RegistroHistorialPedido historial, PedidoMapeador mapeador) {
        this.pedidos = pedidos;
        this.actores = actores;
        this.historial = historial;
        this.mapeador = mapeador;
    }

    @Transactional
    public PedidoResponseDTO validar(Long id, ValidarPedidoRequestDTO dto) {
        Long operadorId = actores.operadorActualId();
        Pedido p = pedidos.findByIdForUpdate(id).orElseThrow(() -> new PedidoNoEncontradoException(id));
        if (Boolean.TRUE.equals(dto.solicitarCorreccion())) {
            if (dto.campoObservado() == null || dto.campoObservado().isBlank())
                throw new IllegalArgumentException("Selecciona el campo que debe corregirse");
            p.solicitarCorreccion(dto.observaciones(), operadorId);
            evento(id, actores.actorActual().getId(), "CORRECCION_SOLICITADA", dto.campoObservado(), dto.observaciones());
        } else {
            if (dto.aprobar() == null) throw new IllegalArgumentException("Debes aprobar, rechazar o solicitar una corrección");
            boolean aprobar = dto.aprobar();
            if (!aprobar && (dto.observaciones() == null || dto.observaciones().isBlank()))
                throw new IllegalArgumentException("Indica el motivo del rechazo");
            p.validar(aprobar, dto.prioridadConfirmada(), dto.observaciones(), dto.justificacionPrioridad(), operadorId);
            evento(id, actores.actorActual().getId(), aprobar ? "PEDIDO_VALIDADO" : "PEDIDO_RECHAZADO", null,
                    dto.justificacionPrioridad() != null ? dto.justificacionPrioridad() : dto.observaciones());
        }
        return mapeador.map(pedidos.save(p));
    }

    private void evento(Long pedidoId, Long usuarioId, String tipo, String campo, String detalle) {
        historial.registrar(pedidoId, usuarioId, tipo, campo, detalle, LocalDateTime.now());
    }
}
