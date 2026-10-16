package com.udea.demo.usuarios.infrastructure.email;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService - infraestructura de envío de correos")
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService service;

    @BeforeEach
    void setUp() {
        service = new EmailService(mailSender);
    }

    @Test
    @DisplayName("Cuando mailEnabled es falso, no envía correos")
    void enviar_deshabilitado_noEnvia() {
        ReflectionTestUtils.setField(service, "mailEnabled", false);

        service.enviarVerificacion("dest@test.com", "Carlos", "http://test.com/link");
        service.enviarEnlaceRestablecimiento("dest@test.com", "http://test.com/reset");
        service.enviarNotificacionCambioEstado("dest@test.com", "Asunto", "Contenido");

        verifyNoInteractions(mailSender);
    }

    @Test
    @DisplayName("Envía correo vía SMTP cuando provider es smtp y remitente está configurado")
    void enviar_smtp_exitoso() {
        ReflectionTestUtils.setField(service, "mailEnabled", true);
        ReflectionTestUtils.setField(service, "provider", "smtp");
        ReflectionTestUtils.setField(service, "from", "remitente@test.com");

        service.enviarVerificacion("dest@test.com", "Carlos", "http://test.com/link");
        service.enviarEnlaceRestablecimiento("dest@test.com", "http://test.com/reset");
        service.enviarNotificacionCambioEstado("dest@test.com", "Asunto Notif", "Mensaje");

        verify(mailSender, times(3)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Usa smtpUsername si from está vacío en modo smtp")
    void enviar_smtp_conUsername_exitoso() {
        ReflectionTestUtils.setField(service, "mailEnabled", true);
        ReflectionTestUtils.setField(service, "provider", "smtp");
        ReflectionTestUtils.setField(service, "from", "");
        ReflectionTestUtils.setField(service, "smtpUsername", "smtpuser@test.com");

        service.enviarNotificacionCambioEstado("dest@test.com", "Asunto", "Texto");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Lanza IllegalStateException si no hay remitente en modo smtp para notificaciones")
    void enviar_smtpSinRemitente_lanzaExcepcion() {
        ReflectionTestUtils.setField(service, "mailEnabled", true);
        ReflectionTestUtils.setField(service, "provider", "smtp");
        ReflectionTestUtils.setField(service, "from", "");
        ReflectionTestUtils.setField(service, "smtpUsername", "");

        assertThatThrownBy(() -> service.enviarNotificacionCambioEstado("dest@test.com", "Asunto", "Texto"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Lanza IllegalStateException si provider no está soportado en notificaciones")
    void enviar_proveedorInvalido_lanzaExcepcion() {
        ReflectionTestUtils.setField(service, "mailEnabled", true);
        ReflectionTestUtils.setField(service, "provider", "desconocido");

        assertThatThrownBy(() -> service.enviarNotificacionCambioEstado("dest@test.com", "Asunto", "Texto"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Captura excepciones del proveedor al enviar verificación sin propagar error crítico")
    void enviar_falloProveedor_noLanza() {
        ReflectionTestUtils.setField(service, "mailEnabled", true);
        ReflectionTestUtils.setField(service, "provider", "smtp");
        ReflectionTestUtils.setField(service, "from", "remitente@test.com");
        doThrow(new RuntimeException("SMTP connect failed")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatNoException().isThrownBy(() ->
                service.enviarVerificacion("dest@test.com", "Carlos", "http://test.com/link"));
    }
}
