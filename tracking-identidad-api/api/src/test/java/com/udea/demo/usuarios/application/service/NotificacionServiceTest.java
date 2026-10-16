package com.udea.demo.usuarios.application.service;

import com.udea.demo.compartido.eventos.EventoIntegracion;
import com.udea.demo.pedidos.domain.model.EstadoPedido;
import com.udea.demo.usuarios.domain.model.*;
import com.udea.demo.usuarios.infrastructure.rabbit.NotificacionRabbitConfig;
import com.udea.demo.usuarios.interfaces.persistence.ClienteRepository;
import com.udea.demo.usuarios.interfaces.persistence.NotificacionEnviadaRepository;
import com.udea.demo.usuarios.interfaces.persistence.PreferenciaNotificacionRepository;
import com.udea.demo.usuarios.interfaces.services.EmailServiceI;
import com.udea.demo.usuarios.interfaces.services.SmsSenderI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificacionService - orquestación de notificaciones multicanal")
class NotificacionServiceTest {

    @Mock private HitoExtractor hitoExtractor;
    @Mock private ClienteRepository clienteRepository;
    @Mock private PreferenciaNotificacionRepository preferenciaRepository;
    @Mock private NotificacionEnviadaRepository notificacionRepository;
    @Mock private EmailServiceI emailService;
    @Mock private SmsSenderI smsSender;
    @Mock private PlantillaMensaje plantilla;
    @Mock private RabbitTemplate rabbitTemplate;

    private NotificacionService service;

    @BeforeEach
    void setUp() {
        service = new NotificacionService(
                hitoExtractor, clienteRepository, preferenciaRepository,
                notificacionRepository, emailService, smsSender,
                plantilla, rabbitTemplate, 3);
    }

    private EventoIntegracion crearEventoDummy(UUID eventId) {
        return new EventoIntegracion() {
            @Override public UUID eventId() { return eventId; }
            @Override public java.time.LocalDateTime ocurridoEn() { return java.time.LocalDateTime.now(); }
            @Override public String tipo() { return "TEST_EVENT"; }
            @Override public String routingKey() { return "pedido.test"; }
        };
    }

    @Test
    @DisplayName("Si el evento no es un hito notificable, no procesa nada")
    void procesar_noHito_ignora() {
        UUID eventId = UUID.randomUUID();
        EventoIntegracion evento = crearEventoDummy(eventId);
        when(hitoExtractor.extraer(evento)).thenReturn(Optional.empty());

        service.procesar(evento);

        verifyNoInteractions(clienteRepository, emailService, smsSender);
    }

    @Test
    @DisplayName("Si el cliente no existe, omite el procesamiento")
    void procesar_clienteNoExiste_omite() {
        UUID eventId = UUID.randomUUID();
        EventoIntegracion evento = crearEventoDummy(eventId);
        HitoNotificacion hito = new HitoNotificacion(1L, "LT123", 99L, EstadoPedido.EN_TRANSITO, null, "CHECKPOINT");
        when(hitoExtractor.extraer(evento)).thenReturn(Optional.of(hito));
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        service.procesar(evento);

        verifyNoInteractions(emailService, smsSender);
    }

    @Nested
    @DisplayName("Notificaciones por Email")
    class NotificacionesEmail {

        @Test
        @DisplayName("Envía email cuando la preferencia incluye EMAIL y no ha sido enviado")
        void procesar_enviaEmailExitoso() {
            UUID eventId = UUID.randomUUID();
            EventoIntegracion evento = crearEventoDummy(eventId);
            HitoNotificacion hito = new HitoNotificacion(1L, "LT123", 10L, EstadoPedido.EN_TRANSITO, null, "CHECKPOINT");

            Usuario usuario = Usuario.builder().email("cliente@test.com").telefono("3001234567").build();
            Cliente cliente = Cliente.builder().id(10L).usuario(usuario).build();

            when(hitoExtractor.extraer(evento)).thenReturn(Optional.of(hito));
            when(clienteRepository.findById(10L)).thenReturn(Optional.of(cliente));
            when(preferenciaRepository.findById(10L)).thenReturn(Optional.empty()); // Default EMAIL

            PlantillaMensaje.Mensaje msg = new PlantillaMensaje.Mensaje("Asunto Tracking", "Cuerpo del mensaje");
            when(plantilla.porEstado("LT123", EstadoPedido.EN_TRANSITO)).thenReturn(msg);

            when(notificacionRepository.findByEventIdAndCanal(eventId.toString(), CanalNotificacion.EMAIL))
                    .thenReturn(Optional.empty());
            when(notificacionRepository.save(any(NotificacionEnviada.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            service.procesar(evento);

            verify(emailService).enviarNotificacionCambioEstado("cliente@test.com", "Asunto Tracking", "Cuerpo del mensaje");
            verify(notificacionRepository, atLeastOnce()).save(any(NotificacionEnviada.class));
        }

        @Test
        @DisplayName("Omite el envío si el email ya fue previamente enviado (idempotencia)")
        void procesar_emailYaEnviado_omite() {
            UUID eventId = UUID.randomUUID();
            EventoIntegracion evento = crearEventoDummy(eventId);
            HitoNotificacion hito = new HitoNotificacion(1L, "LT123", 10L, EstadoPedido.EN_TRANSITO, null, "CHECKPOINT");

            Usuario usuario = Usuario.builder().email("cliente@test.com").build();
            Cliente cliente = Cliente.builder().id(10L).usuario(usuario).build();

            when(hitoExtractor.extraer(evento)).thenReturn(Optional.of(hito));
            when(clienteRepository.findById(10L)).thenReturn(Optional.of(cliente));
            when(preferenciaRepository.findById(10L)).thenReturn(Optional.empty());

            NotificacionEnviada previa = NotificacionEnviada.pendiente(
                    eventId.toString(), "CHECKPOINT", 10L, 1L, "LT123",
                    CanalNotificacion.EMAIL, "cliente@test.com", "Asunto", "Cuerpo");
            previa.marcarEnviada();
            when(notificacionRepository.findByEventIdAndCanal(eventId.toString(), CanalNotificacion.EMAIL))
                    .thenReturn(Optional.of(previa));

            service.procesar(evento);

            verifyNoInteractions(emailService);
        }

        @Test
        @DisplayName("Reintenta y lanza excepción si el fallo es menor al número máximo de intentos")
        void procesar_falloEmail_reintenta() {
            UUID eventId = UUID.randomUUID();
            EventoIntegracion evento = crearEventoDummy(eventId);
            HitoNotificacion hito = new HitoNotificacion(1L, "LT123", 10L, EstadoPedido.EN_TRANSITO, null, "CHECKPOINT");

            Usuario usuario = Usuario.builder().email("cliente@test.com").build();
            Cliente cliente = Cliente.builder().id(10L).usuario(usuario).build();

            when(hitoExtractor.extraer(evento)).thenReturn(Optional.of(hito));
            when(clienteRepository.findById(10L)).thenReturn(Optional.of(cliente));
            when(preferenciaRepository.findById(10L)).thenReturn(Optional.empty());

            PlantillaMensaje.Mensaje msg = new PlantillaMensaje.Mensaje("Asunto", "Cuerpo");
            when(plantilla.porEstado(any(), any())).thenReturn(msg);

            NotificacionEnviada notif = NotificacionEnviada.pendiente(
                    eventId.toString(), "CHECKPOINT", 10L, 1L, "LT123",
                    CanalNotificacion.EMAIL, "cliente@test.com", "Asunto", "Cuerpo");
            when(notificacionRepository.findByEventIdAndCanal(eventId.toString(), CanalNotificacion.EMAIL))
                    .thenReturn(Optional.of(notif));

            doThrow(new RuntimeException("Mail error")).when(emailService)
                    .enviarNotificacionCambioEstado(anyString(), anyString(), anyString());

            assertThatThrownBy(() -> service.procesar(evento))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Mail error");

            assertThat(notif.getIntentos()).isEqualTo(1);
            assertThat(notif.getEstado()).isEqualTo(EstadoNotificacion.FALLIDA);
        }

        @Test
        @DisplayName("Publica en DLQ y no relanza excepción cuando se agotan los intentos máximos")
        void procesar_falloEmailAgotado_publicaEnDlq() {
            UUID eventId = UUID.randomUUID();
            EventoIntegracion evento = crearEventoDummy(eventId);
            HitoNotificacion hito = new HitoNotificacion(1L, "LT123", 10L, EstadoPedido.EN_TRANSITO, null, "CHECKPOINT");

            Usuario usuario = Usuario.builder().email("cliente@test.com").build();
            Cliente cliente = Cliente.builder().id(10L).usuario(usuario).build();

            when(hitoExtractor.extraer(evento)).thenReturn(Optional.of(hito));
            when(clienteRepository.findById(10L)).thenReturn(Optional.of(cliente));
            when(preferenciaRepository.findById(10L)).thenReturn(Optional.empty());

            PlantillaMensaje.Mensaje msg = new PlantillaMensaje.Mensaje("Asunto", "Cuerpo");
            when(plantilla.porEstado(any(), any())).thenReturn(msg);

            NotificacionEnviada notif = NotificacionEnviada.pendiente(
                    eventId.toString(), "CHECKPOINT", 10L, 1L, "LT123",
                    CanalNotificacion.EMAIL, "cliente@test.com", "Asunto", "Cuerpo");
            notif.registrarFallo("Error previo 1");
            notif.registrarFallo("Error previo 2"); // Ahora tiene 2 intentos

            when(notificacionRepository.findByEventIdAndCanal(eventId.toString(), CanalNotificacion.EMAIL))
                    .thenReturn(Optional.of(notif));

            doThrow(new RuntimeException("Mail down")).when(emailService)
                    .enviarNotificacionCambioEstado(anyString(), anyString(), anyString());

            // 3er intento: maxIntentos es 3, por lo que va a DLQ y no relanza
            service.procesar(evento);

            assertThat(notif.getIntentos()).isEqualTo(3);
            verify(rabbitTemplate).convertAndSend(eq(""), eq(NotificacionRabbitConfig.DLQ), anyString());
        }
    }

    @Nested
    @DisplayName("Notificaciones por SMS")
    class NotificacionesSms {

        @Test
        @DisplayName("Envía SMS cuando la preferencia es SMS")
        void procesar_enviaSmsExitoso() {
            UUID eventId = UUID.randomUUID();
            EventoIntegracion evento = crearEventoDummy(eventId);
            HitoNotificacion hito = new HitoNotificacion(1L, "LT123", 10L, EstadoPedido.EN_REPARTO, null, "CHECKPOINT");

            Usuario usuario = Usuario.builder().email("cliente@test.com").telefono("3001112233").build();
            Cliente cliente = Cliente.builder().id(10L).usuario(usuario).build();

            PreferenciaNotificacion pref = new PreferenciaNotificacion(10L);
            pref.actualizar(CanalNotificacion.SMS, "3009998877");

            when(hitoExtractor.extraer(evento)).thenReturn(Optional.of(hito));
            when(clienteRepository.findById(10L)).thenReturn(Optional.of(cliente));
            when(preferenciaRepository.findById(10L)).thenReturn(Optional.of(pref));

            when(plantilla.cuerpoSms(eq("LT123"), anyString())).thenReturn("Mensaje SMS resumen");
            when(notificacionRepository.findByEventIdAndCanal(eventId.toString(), CanalNotificacion.SMS))
                    .thenReturn(Optional.empty());
            when(notificacionRepository.save(any(NotificacionEnviada.class)))
                    .thenAnswer(i -> i.getArgument(0));

            service.procesar(evento);

            verify(smsSender).enviar("3009998877", "Mensaje SMS resumen");
            verifyNoInteractions(emailService);
        }

        @Test
        @DisplayName("Omite SMS si el cliente no tiene número de teléfono registrado")
        void procesar_smsSinTelefono_omite() {
            UUID eventId = UUID.randomUUID();
            EventoIntegracion evento = crearEventoDummy(eventId);
            HitoNotificacion hito = new HitoNotificacion(1L, "LT123", 10L, EstadoPedido.EN_REPARTO, null, "CHECKPOINT");

            Usuario usuario = Usuario.builder().email("cliente@test.com").telefono(null).build();
            Cliente cliente = Cliente.builder().id(10L).usuario(usuario).build();

            PreferenciaNotificacion pref = new PreferenciaNotificacion(10L);
            pref.actualizar(CanalNotificacion.SMS, null);

            when(hitoExtractor.extraer(evento)).thenReturn(Optional.of(hito));
            when(clienteRepository.findById(10L)).thenReturn(Optional.of(cliente));
            when(preferenciaRepository.findById(10L)).thenReturn(Optional.of(pref));

            service.procesar(evento);

            verifyNoInteractions(smsSender);
        }

        @Test
        @DisplayName("Maneja fallo de SMS reintentando si no supera intentos máximos")
        void procesar_falloSms_reintenta() {
            UUID eventId = UUID.randomUUID();
            EventoIntegracion evento = crearEventoDummy(eventId);
            HitoNotificacion hito = new HitoNotificacion(1L, "LT123", 10L, EstadoPedido.ENTREGADO, null, "CHECKPOINT");

            Usuario usuario = Usuario.builder().email("cliente@test.com").telefono("3001112233").build();
            Cliente cliente = Cliente.builder().id(10L).usuario(usuario).build();

            PreferenciaNotificacion pref = new PreferenciaNotificacion(10L);
            pref.actualizar(CanalNotificacion.SMS, "3001112233");

            when(hitoExtractor.extraer(evento)).thenReturn(Optional.of(hito));
            when(clienteRepository.findById(10L)).thenReturn(Optional.of(cliente));
            when(preferenciaRepository.findById(10L)).thenReturn(Optional.of(pref));

            when(plantilla.cuerpoSms(any(), any())).thenReturn("SMS");
            NotificacionEnviada notif = NotificacionEnviada.pendiente(
                    eventId.toString(), "CHECKPOINT", 10L, 1L, "LT123",
                    CanalNotificacion.SMS, "3001112233", null, "SMS");
            when(notificacionRepository.findByEventIdAndCanal(eventId.toString(), CanalNotificacion.SMS))
                    .thenReturn(Optional.of(notif));

            doThrow(new RuntimeException("SMS provider error")).when(smsSender).enviar(anyString(), anyString());

            assertThatThrownBy(() -> service.procesar(evento))
                    .isInstanceOf(RuntimeException.class);
        }
    }
}
