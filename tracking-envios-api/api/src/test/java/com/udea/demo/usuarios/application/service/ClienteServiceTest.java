package com.udea.demo.usuarios.application.service;

import com.udea.demo.usuarios.application.dto.ActualizarPerfilRequestDTO;
import com.udea.demo.usuarios.application.dto.ClienteResponseDTO;
import com.udea.demo.usuarios.application.dto.RegistroClienteRequestDTO;
import com.udea.demo.usuarios.application.dto.UsuarioResponseDTO;
import com.udea.demo.usuarios.domain.model.Cliente;
import com.udea.demo.usuarios.domain.model.EstadoUsuario;
import com.udea.demo.usuarios.domain.model.Rol;
import com.udea.demo.usuarios.domain.model.TokenVerificacion;
import com.udea.demo.usuarios.domain.model.Usuario;
import com.udea.demo.usuarios.interfaces.persistence.ClienteRepository;
import com.udea.demo.usuarios.interfaces.persistence.TokenVerificacionRepository;
import com.udea.demo.usuarios.interfaces.persistence.UsuarioRepository;
import com.udea.demo.usuarios.interfaces.services.EmailServiceI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClienteService - registro y gestión de perfil de clientes")
class ClienteServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private TokenVerificacionRepository tokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailServiceI emailService;
    @Mock private ActorAuthorizationService actorAuthorizationService;
    @Mock private PasswordPolicyService passwordPolicyService;

    @InjectMocks
    private ClienteService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "verificationUrl", "https://app.test/verificar");
    }

    @Nested
    @DisplayName("Registrar cliente")
    class RegistrarCliente {

        @Test
        @DisplayName("Lanza excepción si las contraseñas no coinciden")
        void registrar_passwordsDistintas_lanzaExcepcion() {
            RegistroClienteRequestDTO dto = new RegistroClienteRequestDTO(
                    "Cliente", "c@test.com", "Pass123!", "OtherPass!", "123", "Calle 1", "Bogota", true, "1.0", true, "1.0");

            assertThatThrownBy(() -> service.registrarCliente(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Las contraseñas no coinciden");
        }

        @Test
        @DisplayName("Lanza excepción si el correo ya se encuentra registrado")
        void registrar_correoDuplicado_lanzaExcepcion() {
            RegistroClienteRequestDTO dto = new RegistroClienteRequestDTO(
                    "Cliente", "c@test.com", "Pass123!", "Pass123!", "123", "Calle 1", "Bogota", true, "1.0", true, "1.0");
            when(usuarioRepository.existsByEmail("c@test.com")).thenReturn(true);

            assertThatThrownBy(() -> service.registrarCliente(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("El correo ya se encuentra registrado");
        }

        @Test
        @DisplayName("Registra exitosamente cliente y despacha correo de verificación")
        void registrar_exitoso_creaClienteYToken() {
            RegistroClienteRequestDTO dto = new RegistroClienteRequestDTO(
                    "Carlos Gomez", "carlos@test.com", "ClaveSegura1!", "ClaveSegura1!",
                    "3001234567", "Carrera 7 # 45-10", "Medellin", true, "1.0", true, "1.0");

            when(usuarioRepository.existsByEmail("carlos@test.com")).thenReturn(false);
            when(passwordEncoder.encode("ClaveSegura1!")).thenReturn("hashedPass");

            Usuario usuarioGuardado = Usuario.builder()
                    .id(101L)
                    .nombre(dto.nombre())
                    .email(dto.email())
                    .rol(Rol.CLIENTE)
                    .estado(EstadoUsuario.PENDIENTE_VERIFICACION)
                    .build();
            when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioGuardado);

            when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> {
                Cliente c = invocation.getArgument(0);
                c.setId(201L);
                return c;
            });

            ClienteResponseDTO res = service.registrarCliente(dto);

            assertThat(res.idCliente()).isEqualTo(201L);
            assertThat(res.email()).isEqualTo("carlos@test.com");
            assertThat(res.ciudad()).isEqualTo("Medellin");
            verify(tokenRepository).save(any(TokenVerificacion.class));
            verify(emailService).enviarVerificacion(eq("carlos@test.com"), eq("Carlos Gomez"), anyString());
        }
    }

    @Nested
    @DisplayName("Reenviar verificación")
    class ReenviarVerificacion {

        @Test
        @DisplayName("Genera nuevo token si el usuario está pendiente y no se ha enviado en el último minuto")
        void reenviarVerificacion_exitoso() {
            Usuario usuario = Usuario.builder()
                    .id(50L).email("c@test.com").nombre("Carlos")
                    .rol(Rol.CLIENTE).activo(true).estado(EstadoUsuario.PENDIENTE_VERIFICACION).build();
            when(usuarioRepository.findByEmail("c@test.com")).thenReturn(Optional.of(usuario));
            when(tokenRepository.findByUsuarioId(50L)).thenReturn(Optional.empty());

            service.reenviarVerificacion("c@test.com");

            verify(tokenRepository).save(any(TokenVerificacion.class));
            verify(emailService).enviarVerificacion(eq("c@test.com"), eq("Carlos"), anyString());
        }

        @Test
        @DisplayName("Omite reenvío si se envió hace menos de 1 minuto")
        void reenviarVerificacion_reciente_noHaceNada() {
            Usuario usuario = Usuario.builder()
                    .id(50L).email("c@test.com").nombre("Carlos")
                    .rol(Rol.CLIENTE).activo(true).estado(EstadoUsuario.PENDIENTE_VERIFICACION).build();
            TokenVerificacion tokenPrevio = TokenVerificacion.builder()
                    .usuario(usuario)
                    .fechaExpiracion(LocalDateTime.now().plusHours(2)) // Creado hace instantes
                    .build();

            when(usuarioRepository.findByEmail("c@test.com")).thenReturn(Optional.of(usuario));
            when(tokenRepository.findByUsuarioId(50L)).thenReturn(Optional.of(tokenPrevio));

            service.reenviarVerificacion("c@test.com");

            verify(emailService, never()).enviarVerificacion(anyString(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Verificar cuenta")
    class VerificarCuenta {

        @Test
        @DisplayName("Lanza excepción si el token no existe")
        void verificarCuenta_tokenInexistente_lanzaExcepcion() {
            when(tokenRepository.findByToken(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.verificarCuenta("tokenInvalido"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Token de verificación inválido");
        }

        @Test
        @DisplayName("Lanza excepción si el token está expirado")
        void verificarCuenta_tokenExpirado_lanzaExcepcion() {
            TokenVerificacion tv = TokenVerificacion.builder()
                    .fechaExpiracion(LocalDateTime.now().minusHours(1))
                    .build();
            when(tokenRepository.findByToken(anyString())).thenReturn(Optional.of(tv));

            assertThatThrownBy(() -> service.verificarCuenta("tokenExp"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ha expirado");
        }

        @Test
        @DisplayName("Lanza excepción si la cuenta ya no está pendiente de verificación")
        void verificarCuenta_estadoNoPendiente_lanzaExcepcion() {
            Usuario usuario = Usuario.builder()
                    .id(1L).rol(Rol.CLIENTE).activo(true).estado(EstadoUsuario.ACTIVO).build();
            TokenVerificacion tv = TokenVerificacion.builder()
                    .usuario(usuario)
                    .fechaExpiracion(LocalDateTime.now().plusHours(1))
                    .build();
            when(tokenRepository.findByToken(anyString())).thenReturn(Optional.of(tv));

            assertThatThrownBy(() -> service.verificarCuenta("tokenValido"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ya no corresponde");
        }

        @Test
        @DisplayName("Activa la cuenta y elimina el token de verificación")
        void verificarCuenta_exitoso_activaUsuario() {
            Usuario usuario = Usuario.builder()
                    .id(1L).rol(Rol.CLIENTE).activo(true).estado(EstadoUsuario.PENDIENTE_VERIFICACION).build();
            TokenVerificacion tv = TokenVerificacion.builder()
                    .usuario(usuario)
                    .fechaExpiracion(LocalDateTime.now().plusHours(1))
                    .build();
            when(tokenRepository.findByToken(anyString())).thenReturn(Optional.of(tv));

            service.verificarCuenta("tokenValido");

            assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
            verify(usuarioRepository).save(usuario);
            verify(tokenRepository).delete(tv);
        }
    }

    @Nested
    @DisplayName("Consultar y actualizar perfil")
    class PerfilCliente {

        @Test
        @DisplayName("Obtiene el perfil del cliente autenticado")
        void obtenerPerfilActual_exitoso() {
            Usuario u = Usuario.builder().id(10L).nombre("Ana").email("ana@test.com")
                    .telefono("311").direccion("Calle 5").rol(Rol.CLIENTE).estado(EstadoUsuario.ACTIVO).build();
            Cliente c = Cliente.builder().id(20L).usuario(u).ciudad("Cali").build();
            when(actorAuthorizationService.clienteActualId()).thenReturn(20L);
            when(clienteRepository.findById(20L)).thenReturn(Optional.of(c));

            ClienteResponseDTO res = service.obtenerPerfilActual();

            assertThat(res.idCliente()).isEqualTo(20L);
            assertThat(res.nombre()).isEqualTo("Ana");
            assertThat(res.ciudad()).isEqualTo("Cali");
        }

        @Test
        @DisplayName("Actualiza el perfil del cliente autenticado")
        void actualizarPerfilActual_exitoso() {
            Usuario u = Usuario.builder().id(10L).nombre("Viejo").email("ana@test.com").rol(Rol.CLIENTE).build();
            when(actorAuthorizationService.actorActual()).thenReturn(u);
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

            ActualizarPerfilRequestDTO dto = new ActualizarPerfilRequestDTO("Nuevo", "322", "Nueva Dir");
            UsuarioResponseDTO res = service.actualizarPerfilActual(dto);

            assertThat(res.nombre()).isEqualTo("Nuevo");
            assertThat(res.telefono()).isEqualTo("322");
            assertThat(res.direccion()).isEqualTo("Nueva Dir");
        }

        @Test
        @DisplayName("Desactiva la cuenta del cliente autenticado")
        void desactivarCuentaActual_exitoso() {
            Usuario u = Usuario.builder().id(10L).rol(Rol.CLIENTE).activo(true).estado(EstadoUsuario.ACTIVO).build();
            when(actorAuthorizationService.actorActual()).thenReturn(u);

            service.desactivarCuentaActual();

            assertThat(u.getActivo()).isFalse();
            assertThat(u.getEstado()).isEqualTo(EstadoUsuario.INACTIVO);
            verify(usuarioRepository).save(u);
        }

        @Test
        @DisplayName("Lanza excepción si intenta desactivar cuenta y el rol no es cliente")
        void desactivarCuentaActual_noCliente_lanzaExcepcion() {
            Usuario u = Usuario.builder().id(10L).rol(Rol.OPERADOR).build();
            when(actorAuthorizationService.actorActual()).thenReturn(u);

            assertThatThrownBy(() -> service.desactivarCuentaActual())
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Actualiza perfil por id verificando propietario")
        void actualizarPerfil_porId_exitoso() {
            Usuario u = Usuario.builder().id(15L).email("c@test.com").nombre("Ana").rol(Rol.CLIENTE).build();
            when(usuarioRepository.findById(15L)).thenReturn(Optional.of(u));
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

            ActualizarPerfilRequestDTO dto = new ActualizarPerfilRequestDTO("Ana Maria", "333", "Dir");
            UsuarioResponseDTO res = service.actualizarPerfil(15L, dto, "c@test.com");

            assertThat(res.nombre()).isEqualTo("Ana Maria");
            verify(actorAuthorizationService).exigirPropietario(15L, Rol.CLIENTE);
        }

        @Test
        @DisplayName("Lanza AccessDeniedException si el email autenticado no coincide con el usuario a editar")
        void actualizarPerfil_noPropietario_lanzaAccessDenied() {
            Usuario u = Usuario.builder().id(15L).email("c@test.com").build();
            when(usuarioRepository.findById(15L)).thenReturn(Optional.of(u));

            ActualizarPerfilRequestDTO dto = new ActualizarPerfilRequestDTO("Ana", "333", "Dir");
            assertThatThrownBy(() -> service.actualizarPerfil(15L, dto, "hacker@test.com"))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("Desactiva cuenta cliente por id verificando propietario")
        void desactivarCuentaCliente_porId_exitoso() {
            Usuario u = Usuario.builder().id(15L).email("c@test.com").rol(Rol.CLIENTE).activo(true).build();
            when(usuarioRepository.findById(15L)).thenReturn(Optional.of(u));

            service.desactivarCuentaCliente(15L, "c@test.com");

            assertThat(u.getActivo()).isFalse();
            assertThat(u.getEstado()).isEqualTo(EstadoUsuario.INACTIVO);
            verify(actorAuthorizationService).exigirPropietario(15L, Rol.CLIENTE);
            verify(usuarioRepository).save(u);
        }
    }
}
