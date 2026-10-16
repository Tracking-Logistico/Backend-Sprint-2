package com.udea.demo.usuarios.application.service;

import com.udea.demo.pedidos.domain.model.EstadoPedido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PlantillaMensaje - generación de mensajes para email y SMS")
class PlantillaMensajeTest {

    private final PlantillaMensaje plantilla = new PlantillaMensaje("http://localhost:5173/tracking/");

    @ParameterizedTest
    @EnumSource(value = EstadoPedido.class, names = {"EN_TRANSITO", "EN_REPARTO", "ENTREGADO", "ENTREGA_FALLIDA", "ENTREGA_REPROGRAMADA", "CREADO"})
    @DisplayName("Genera mensajes legibles por estado con enlace directo")
    void porEstado_generaMensajeValido(EstadoPedido estado) {
        PlantillaMensaje.Mensaje msg = plantilla.porEstado("LT1234567890", estado);

        assertThat(msg.asunto()).contains("LT1234567890");
        assertThat(msg.cuerpo()).contains("http://localhost:5173/tracking?code=LT1234567890");
        assertThat(msg.cuerpo()).contains("Tracking Logístico");
    }

    @Test
    @DisplayName("Genera mensaje personalizado cuando viene redactado por el sistema")
    void porMensajeCliente_generaMensaje() {
        PlantillaMensaje.Mensaje msg = plantilla.porMensajeCliente("LT999", "Tu pedido está listo para recoger");

        assertThat(msg.asunto()).contains("LT999");
        assertThat(msg.cuerpo()).contains("Tu pedido está listo para recoger");
        assertThat(msg.cuerpo()).contains("?code=LT999");
    }

    @Test
    @DisplayName("Genera cuerpo SMS sin datos sensibles")
    void cuerpoSms_generaTextoCompacto() {
        String sms = plantilla.cuerpoSms("LT888", "en reparto.");

        assertThat(sms).contains("Tracking Logístico: en reparto.");
        assertThat(sms).contains("LT888");
        assertThat(sms).contains("http://localhost:5173/tracking?code=LT888");
    }
}
