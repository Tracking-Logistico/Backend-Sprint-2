package com.udea.demo.pedidos.application.service;

import com.udea.demo.pedidos.application.dto.*;
import com.udea.demo.pedidos.domain.exception.PedidoNoEncontradoException;
import com.udea.demo.pedidos.domain.model.*;
import com.udea.demo.pedidos.domain.service.PrioridadStrategy;
import com.udea.demo.pedidos.interfaces.persistence.PedidoRepository;
import com.udea.demo.usuarios.application.service.ActorAuthorizationService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class RecepcionPedidoUseCase {
    private final PedidoRepository pedidos;
    private final PrioridadStrategy prioridadStrategy;
    private final GeneradorNumeroPedido numerosPedido;
    private final ActorAuthorizationService actores;
    private final LimitesServicioService limites;
    private final RegistroHistorialPedido historial;
    private final PedidoMapeador mapeador;

    public RecepcionPedidoUseCase(PedidoRepository pedidos, PrioridadStrategy prioridadStrategy,
            GeneradorNumeroPedido numerosPedido, ActorAuthorizationService actores,
            LimitesServicioService limites, RegistroHistorialPedido historial, PedidoMapeador mapeador) {
        this.pedidos = pedidos;
        this.prioridadStrategy = prioridadStrategy;
        this.numerosPedido = numerosPedido;
        this.actores = actores;
        this.limites = limites;
        this.historial = historial;
        this.mapeador = mapeador;
    }

    @Transactional
    public PedidoResponseDTO recibir(RecibirPedidoRequestDTO dto) {
        limites.validar(dto);
        Long clienteId = actores.clienteActualId();
        var remitente = actores.actorActual();
        Prioridad sugerida = prioridadStrategy.sugerir(dto.tipoServicio(), dto.pesoKg());
        Pedido pedido = Pedido.recibir(clienteId, dto.direccionOrigen(), dto.ciudadOrigen(), dto.codigoPostalOrigen(),
                dto.direccionDestino(), dto.ciudadDestino(), dto.codigoPostalDestino(),
                dto.descripcionPaquete(), dto.pesoKg(), dto.largoCm(), dto.anchoCm(), dto.altoCm(),
                dto.tipoServicio(), generarNumeroPedidoUnico(), sugerida, dto.destinatarioNombre(), dto.destinatarioTelefono(),
                remitente.getNombre(), remitente.getEmail(), dto.remitenteTelefono(), dto.destinatarioEmail());
        Pedido guardado = pedidos.save(pedido);
        evento(guardado.getId(), actores.actorActual().getId(), "PEDIDO_SOLICITADO", null,
                "Pedido creado por el cliente");
        return mapeador.map(guardado);
    }

    @Transactional
    public PedidoResponseDTO corregir(Long id, RecibirPedidoRequestDTO dto) {
        limites.validar(dto);
        Pedido p = pedidos.findByIdForUpdate(id).orElseThrow(() -> new PedidoNoEncontradoException(id));
        if (!p.getClienteId().equals(actores.clienteActualId())) throw new AccessDeniedException("El pedido no pertenece al cliente autenticado");
        Prioridad sugerida = prioridadStrategy.sugerir(dto.tipoServicio(), dto.pesoKg());
        p.corregir(dto.direccionOrigen(), dto.ciudadOrigen(), dto.codigoPostalOrigen(), dto.direccionDestino(),
                dto.ciudadDestino(), dto.codigoPostalDestino(), dto.descripcionPaquete(), dto.pesoKg(), dto.largoCm(),
                dto.anchoCm(), dto.altoCm(), dto.tipoServicio(), dto.destinatarioNombre(), dto.destinatarioTelefono(),
                dto.remitenteTelefono(), sugerida, dto.destinatarioEmail());
        evento(id, actores.actorActual().getId(), "PEDIDO_CORREGIDO", null, "El cliente actualizó la información observada");
        return mapeador.map(pedidos.save(p));
    }

    private String generarNumeroPedidoUnico() {
        for (int i = 0; i < 10; i++) {
            String numero = numerosPedido.generar();
            if (!pedidos.existsByNumeroPedido(numero)) return numero;
        }
        throw new IllegalStateException("No fue posible generar un identificador único de pedido");
    }

    private void evento(Long pedidoId, Long usuarioId, String tipo, String campo, String detalle) {
        historial.registrar(pedidoId, usuarioId, tipo, campo, detalle, LocalDateTime.now());
    }
}
