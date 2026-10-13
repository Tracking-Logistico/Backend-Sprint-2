package com.udea.demo.pedidos.domain.event;

import com.udea.demo.compartido.eventos.EventoIntegracion;
import com.udea.demo.pedidos.domain.model.EstadoPedido;
import com.udea.demo.pedidos.domain.model.Pedido;
import java.time.LocalDateTime;
import java.util.UUID;

/** Notifica una entrega registrada desde el panel, sin requerir un escaneo QR artificial. */
public record EntregaConfirmadaEvent(UUID eventId, String tipo, int version,
        LocalDateTime ocurridoEn, Long pedidoId, String numeroTracking,
        Long clienteId, EstadoPedido estadoActual) implements EventoIntegracion {
    public static final String TIPO = "ENTREGA_CONFIRMADA";
    public static final String ROUTING_KEY = "pedido.entrega.confirmada";
    public static EntregaConfirmadaEvent de(Pedido pedido) {
        return new EntregaConfirmadaEvent(UUID.randomUUID(), TIPO, 1,
                LocalDateTime.now(), pedido.getId(), pedido.getNumeroTracking(),
                pedido.getClienteId(), EstadoPedido.ENTREGADO);
    }
    @Override public String routingKey() { return ROUTING_KEY; }
}
