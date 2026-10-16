package com.udea.demo.config;

import com.udea.demo.compartido.eventos.EventoIntegracion;
import com.udea.demo.config.messaging.LogEventoIntegracionPublisher;
import com.udea.demo.config.messaging.RabbitEventoIntegracionPublisher;
import com.udea.demo.config.messaging.RabbitMQConfig;
import com.udea.demo.pedidos.domain.exception.PedidoNoEncontradoException;
import com.udea.demo.pedidos.domain.exception.TrackingNoActivoException;
import com.udea.demo.pedidos.domain.exception.TransicionEstadoInvalidaException;
import com.udea.demo.usuarios.application.service.AutenticacionService;
import com.udea.demo.usuarios.domain.exception.EntregaCorreoException;
import com.udea.demo.usuarios.domain.exception.PerfilRolIncompletoException;
import com.udea.demo.usuarios.domain.model.EstadoUsuario;
import com.udea.demo.usuarios.domain.model.Rol;
import com.udea.demo.usuarios.domain.model.SesionUsuario;
import com.udea.demo.usuarios.domain.model.Usuario;
import com.udea.demo.usuarios.interfaces.persistence.IntentoInicioSesionRepository;
import com.udea.demo.usuarios.interfaces.persistence.SesionUsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConfigCoreTest - pruebas de configuración, seguridad y mensajería")
class ConfigCoreTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("GlobalExceptionHandler - mapeo HTTP de excepciones")
    class ExceptionHandlerTests {

        private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

        @Test
        @DisplayName("Mapea EntregaCorreoException a 503 SERVICE_UNAVAILABLE")
        void testEntregaCorreoException() {
            ResponseEntity<Map<String, String>> res = handler.mailUnavailable(new EntregaCorreoException());
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            assertThat(res.getBody()).containsKey("error");
        }

        @Test
        @DisplayName("Mapea PerfilRolIncompletoException a 409 CONFLICT")
        void testPerfilIncompleto() {
            ResponseEntity<Map<String, String>> res = handler.perfilIncompleto(new PerfilRolIncompletoException(Rol.OPERADOR, 1L));
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(res.getBody()).containsEntry("code", "PERFIL_ROL_INCOMPLETO");
        }

        @Test
        @DisplayName("Mapea AccessDeniedException a 403 FORBIDDEN")
        void testAccessDenied() {
            ResponseEntity<Map<String, String>> res = handler.forbidden(new AccessDeniedException("Denegado"));
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("Mapea IllegalArgumentException a 400 BAD_REQUEST")
        void testIllegalArgument() {
            ResponseEntity<Map<String, String>> res = handler.invalidBusinessInput(new IllegalArgumentException("Dato invalido"));
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Mapea PedidoNoEncontradoException a 404 NOT_FOUND")
        void testOrderMissing() {
            ResponseEntity<Map<String, String>> res = handler.orderMissing(new PedidoNoEncontradoException(1L));
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Mapea TransicionEstadoInvalidaException y TrackingNoActivoException a 409 CONFLICT")
        void testOrderConflict() {
            ResponseEntity<Map<String, String>> res1 = handler.orderConflict(
                    new TransicionEstadoInvalidaException(com.udea.demo.pedidos.domain.model.EstadoPedido.CREADO, com.udea.demo.pedidos.domain.model.EstadoPedido.ENTREGADO));
            assertThat(res1.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

            ResponseEntity<Map<String, String>> res2 = handler.orderConflict(new TrackingNoActivoException(1L));
            assertThat(res2.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("Mapea IllegalStateException con y sin causa")
        void testIllegalState() {
            ResponseEntity<Map<String, String>> res1 = handler.invalidState(new IllegalStateException("Sin causa"));
            assertThat(res1.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

            ResponseEntity<Map<String, String>> res2 = handler.invalidState(new IllegalStateException("Con causa", new RuntimeException()));
            assertThat(res2.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        @DisplayName("Mapea OptimisticLockingFailureException a 409 CONFLICT")
        void testConcurrentUpdate() {
            ResponseEntity<Map<String, String>> res = handler.concurrentUpdate(new OptimisticLockingFailureException("Concurrente"));
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("Mapea DataIntegrityViolationException a 409 CONFLICT")
        void testIntegrityError() {
            ResponseEntity<Map<String, String>> res = handler.integrityError(new DataIntegrityViolationException("Restriccion"));
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("Mapea ResponseStatusException y NoResourceFoundException")
        void testStatusAndNotFound() {
            ResponseEntity<Map<String, String>> res1 = handler.statusException(new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY));
            assertThat(res1.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

            NoResourceFoundException nrf = mock(NoResourceFoundException.class);
            ResponseEntity<Map<String, String>> res2 = handler.resourceMissing(nrf);
            assertThat(res2.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Mapea Exception general a 500 INTERNAL_SERVER_ERROR")
        void testUnexpected() {
            ResponseEntity<Map<String, String>> res = handler.unexpected(new Exception("Error fatal"));
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        @DisplayName("Mapea MethodArgumentNotValidException con detalle de campos")
        void testValidationException() {
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            BindingResult br = mock(BindingResult.class);
            when(ex.getBindingResult()).thenReturn(br);
            when(br.getFieldErrors()).thenReturn(List.of(new FieldError("obj", "email", "Formato inválido")));

            ResponseEntity<Map<String, String>> res = handler.fields(ex);
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(res.getBody()).containsEntry("email", "Formato inválido");
        }
    }

    @Nested
    @DisplayName("Filtros de Seguridad")
    class FiltrosSeguridad {

        @Mock private SesionUsuarioRepository sesionRepository;
        @Mock private FilterChain filterChain;

        @Test
        @DisplayName("SesionAuthenticationFilter establece autenticación con token Bearer válido")
        void testSesionFilter_tokenValido() throws ServletException, IOException {
            SesionAuthenticationFilter filter = new SesionAuthenticationFilter(sesionRepository);
            ReflectionTestUtils.setField(filter, "minutosInactividadCliente", 30L);

            Usuario usuario = Usuario.builder()
                    .email("cliente@test.com")
                    .rol(Rol.CLIENTE)
                    .estado(EstadoUsuario.ACTIVO)
                    .activo(true)
                    .build();
            LocalDateTime ahora = LocalDateTime.now();
            SesionUsuario sesion = new SesionUsuario(
                    usuario, AutenticacionService.hash("miToken"), "refHash",
                    ahora.plusMinutes(20), ahora.plusDays(7), ahora.minusSeconds(10));

            when(sesionRepository.findByAccessTokenHash(AutenticacionService.hash("miToken")))
                    .thenReturn(Optional.of(sesion));

            MockHttpServletRequest req = new MockHttpServletRequest();
            req.setRequestURI("/api/v1/pedidos");
            req.addHeader("Authorization", "Bearer miToken");
            MockHttpServletResponse res = new MockHttpServletResponse();

            filter.doFilter(req, res, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo("cliente@test.com");
            verify(filterChain).doFilter(req, res);
        }

        @Test
        @DisplayName("SesionAuthenticationFilter asigna ROLE_PASSWORD_CHANGE si está en PENDIENTE_ACTIVACION")
        void testSesionFilter_pendienteActivacion() throws ServletException, IOException {
            SesionAuthenticationFilter filter = new SesionAuthenticationFilter(sesionRepository);
            ReflectionTestUtils.setField(filter, "minutosInactividadOperativo", 15L);

            Usuario usuario = Usuario.builder()
                    .email("operador@test.com")
                    .rol(Rol.OPERADOR)
                    .estado(EstadoUsuario.PENDIENTE_ACTIVACION)
                    .activo(true)
                    .build();
            LocalDateTime ahora = LocalDateTime.now();
            SesionUsuario sesion = new SesionUsuario(
                    usuario, AutenticacionService.hash("miTokenOp"), "refHash",
                    ahora.plusMinutes(10), ahora.plusDays(7), ahora.minusSeconds(10));

            when(sesionRepository.findByAccessTokenHash(AutenticacionService.hash("miTokenOp")))
                    .thenReturn(Optional.of(sesion));

            MockHttpServletRequest req = new MockHttpServletRequest();
            req.setRequestURI("/api/v1/auth/cambiar-password");
            req.addHeader("Authorization", "Bearer miTokenOp");
            MockHttpServletResponse res = new MockHttpServletResponse();

            filter.doFilter(req, res, filterChain);

            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PASSWORD_CHANGE"))).isTrue();
        }

        @Test
        @DisplayName("LoginRateLimitFilter limita tras superar número de intentos por minuto")
        void testLoginRateLimitFilter() throws ServletException, IOException {
            LoginRateLimitFilter filter = new LoginRateLimitFilter();
            ReflectionTestUtils.setField(filter, "maxPorMinuto", 2);

            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            req.setRemoteAddr("192.168.1.100");
            MockHttpServletResponse res = new MockHttpServletResponse();

            // Intento 1
            filter.doFilter(req, res, filterChain);
            assertThat(res.getStatus()).isEqualTo(HttpStatus.OK.value());

            // Intento 2
            res = new MockHttpServletResponse();
            filter.doFilter(req, res, filterChain);
            assertThat(res.getStatus()).isEqualTo(HttpStatus.OK.value());

            // Intento 3 (bloqueado)
            res = new MockHttpServletResponse();
            filter.doFilter(req, res, filterChain);
            assertThat(res.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        }

        @Test
        @DisplayName("DbaApiKeyAuthenticationFilter autentica con header X-DBA-Key correcto")
        void testDbaFilter() throws ServletException, IOException {
            DbaApiKeyAuthenticationFilter filter = new DbaApiKeyAuthenticationFilter();
            ReflectionTestUtils.setField(filter, "configuredKey", "secret-dba-key");

            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/admin/metricas");
            req.addHeader("X-DBA-Key", "secret-dba-key");
            MockHttpServletResponse res = new MockHttpServletResponse();

            filter.doFilter(req, res, filterChain);

            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_DBA"))).isTrue();
        }
    }

    @Nested
    @DisplayName("TareasMantenimiento y Mensajería")
    class MantenimientoYMessaging {

        @Mock private IntentoInicioSesionRepository intentoRepository;
        @Mock private SesionUsuarioRepository sesionRepository;
        @Mock private RabbitTemplate rabbitTemplate;
        @Mock private ConnectionFactory connectionFactory;

        @Test
        @DisplayName("TareasMantenimientoSeguridad purga intentos y sesiones caducadas")
        void testTareasMantenimiento() {
            TareasMantenimientoSeguridad tareas = new TareasMantenimientoSeguridad(intentoRepository, sesionRepository);
            tareas.purgarDatosCaducados();

            verify(intentoRepository).deleteByIntentadoEnBefore(any(LocalDateTime.class));
            verify(sesionRepository).deleteByRefreshTokenExpiresAtBefore(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("LogEventoIntegracionPublisher publica en logs sin fallar")
        void testLogPublisher() {
            LogEventoIntegracionPublisher pub = new LogEventoIntegracionPublisher();
            EventoIntegracion evento = mock(EventoIntegracion.class);
            when(evento.tipo()).thenReturn("EVT_TIPO");
            when(evento.eventId()).thenReturn(UUID.randomUUID());
            when(evento.routingKey()).thenReturn("pedido.creado");

            pub.publicar(evento);
        }

        @Test
        @DisplayName("RabbitEventoIntegracionPublisher publica evento y captura AmqpException")
        void testRabbitPublisher() {
            RabbitEventoIntegracionPublisher pub = new RabbitEventoIntegracionPublisher(rabbitTemplate, "logistica.eventos");
            EventoIntegracion evento = mock(EventoIntegracion.class);
            when(evento.routingKey()).thenReturn("pedido.creado");
            when(evento.eventId()).thenReturn(UUID.randomUUID());
            when(evento.tipo()).thenReturn("CREADO");

            pub.publicar(evento);
            verify(rabbitTemplate).convertAndSend(eq("logistica.eventos"), eq("pedido.creado"), eq(evento), any(org.springframework.amqp.core.MessagePostProcessor.class));

            doThrow(new AmqpException("Broker down")).when(rabbitTemplate)
                    .convertAndSend(anyString(), anyString(), any(Object.class), any(org.springframework.amqp.core.MessagePostProcessor.class));
            pub.publicar(evento); // no lanza excepción
        }

        @Test
        @DisplayName("RabbitMQConfig declara beans de exchange, converter y admin")
        void testRabbitMQConfig() {
            RabbitMQConfig cfg = new RabbitMQConfig();
            assertThat(cfg.eventosLogisticaExchange("mi.exchange").getName()).isEqualTo("mi.exchange");
            MessageConverter conv = cfg.eventosMessageConverter();
            assertThat(conv).isNotNull();
            assertThat(cfg.rabbitAdmin(connectionFactory)).isNotNull();
        }
    }
}
