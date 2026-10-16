package com.udea.demo.pedidos.application.service;

import com.udea.demo.pedidos.domain.model.EstadoPedido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MensajesSeguimientoCliente - traducción amigable de estados hacia el cliente")
class MensajesSeguimientoClienteTest {

    @Test
    @DisplayName("Traduce todos los estados conocidos a mensajes claros")
    void traducirEstados() {
        assertThat(MensajesSeguimientoCliente.estado(EstadoPedido.CREADO)).contains("registrado y está listo");
        assertThat(MensajesSeguimientoCliente.estado(EstadoPedido.RECIBIDO_EN_ORIGEN)).contains("centro de origen");
        assertThat(MensajesSeguimientoCliente.estado(EstadoPedido.EN_TRANSITO)).contains("en camino hacia la ciudad de destino");
        assertThat(MensajesSeguimientoCliente.estado(EstadoPedido.EN_REPARTO)).contains("salió a reparto");
        assertThat(MensajesSeguimientoCliente.estado(EstadoPedido.ENTREGADO)).contains("fue entregado");
        assertThat(MensajesSeguimientoCliente.estado(EstadoPedido.ENTREGA_FALLIDA)).contains("puedes elegir una nueva fecha");
        assertThat(MensajesSeguimientoCliente.estado(EstadoPedido.ENTREGA_REPROGRAMADA)).contains("fue reprogramada");
        assertThat(MensajesSeguimientoCliente.estado(EstadoPedido.DIRECCION_POR_VERIFICAR)).contains("confirmes la dirección");
        assertThat(MensajesSeguimientoCliente.estado(EstadoPedido.DEVOLUCION_AL_REMITENTE)).contains("devuelto al remitente");
        assertThat(MensajesSeguimientoCliente.estado(EstadoPedido.ENTREGA_FALLIDA_CERRADA)).contains("caso fue cerrado");
        assertThat(MensajesSeguimientoCliente.estado(EstadoPedido.RECHAZADO)).contains("no fue aprobada");
        assertThat(MensajesSeguimientoCliente.estado(EstadoPedido.SOLICITADO)).contains("en proceso");
        assertThat(MensajesSeguimientoCliente.PUNTO_CONTROL).contains("punto de control");
    }
}
