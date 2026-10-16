package com.udea.demo.usuarios.infrastructure.sms;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SmsMockSender - emulador de envío de SMS")
class SmsMockSenderTest {

    private final SmsMockSender sender = new SmsMockSender();

    @Test
    @DisplayName("Cuando smsEnabled es false, registra deshabilitado y retorna true")
    void enviar_deshabilitado_retornaTrue() {
        ReflectionTestUtils.setField(sender, "smsEnabled", false);

        boolean enviado = sender.enviar("+573001234567", "Mensaje de prueba");

        assertThat(enviado).isTrue();
    }

    @Test
    @DisplayName("Cuando smsEnabled es true, registra enviado y retorna true con enmascaramiento")
    void enviar_habilitado_retornaTrue() {
        ReflectionTestUtils.setField(sender, "smsEnabled", true);

        boolean enviado = sender.enviar("+573001234567", "Mensaje de prueba");

        assertThat(enviado).isTrue();

        // Número corto o nulo
        assertThat(sender.enviar("12", "Msg")).isTrue();
        assertThat(sender.enviar(null, "Msg")).isTrue();
    }
}
