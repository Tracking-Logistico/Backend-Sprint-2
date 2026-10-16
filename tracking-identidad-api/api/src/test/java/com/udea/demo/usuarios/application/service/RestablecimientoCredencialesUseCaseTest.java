package com.udea.demo.usuarios.application.service;

import com.udea.demo.usuarios.application.dto.RestablecerPasswordDTO;
import com.udea.demo.usuarios.application.dto.SolicitarRestablecimientoPasswordDTO;
import com.udea.demo.usuarios.domain.exception.PasswordNoCoincideException;
import com.udea.demo.usuarios.domain.exception.TokenRestablecimientoInvalidoException;
import com.udea.demo.usuarios.domain.model.EstadoUsuario;
import com.udea.demo.usuarios.domain.model.TokenRestablecimientoPassword;
import com.udea.demo.usuarios.domain.model.Usuario;
import com.udea.demo.usuarios.interfaces.persistence.IntentoInicioSesionRepository;
import com.udea.demo.usuarios.interfaces.persistence.SesionUsuarioRepository;
import com.udea.demo.usuarios.interfaces.persistence.TokenRestablecimientoPasswordRepository;
import com.udea.demo.usuarios.interfaces.persistence.UsuarioRepository;
import com.udea.demo.usuarios.interfaces.services.EmailServiceI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
@DisplayName("RestablecimientoCredencialesUseCase - recuperación de contraseña")
class RestablecimientoCredencialesUseCaseTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private IntentoInicioSesionRepository intentoRepository;
    @Mock private SesionUsuarioRepository sesionRepository;
    @Mock private TokenRestablecimientoPasswordRepository resetRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailServiceI emailService;
    @Mock private PasswordPolicyService passwordPolicyService;

    @InjectMocks
    private RestablecimientoCredencialesUseCase useCase;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(useCase, "minutosReset", 45L);
        ReflectionTestUtils.setField(useCase, "passwordResetUrl", "http://app.test/reset");
    }

    @Test
    @DisplayName("Solicitar restablecimiento genera token y envía correo si el usuario existe")
    void solicitarRestablecimiento_usuarioExiste_enviaCorreo() {
        Usuario usuario = Usuario.builder().id(10L).email("user@test.com").build();
        when(usuarioRepository.findByEmail("user@test.com")).thenReturn(Optional.of(usuario));

        useCase.solicitarRestablecimiento(new SolicitarRestablecimientoPasswordDTO("user@test.com"));

        verify(resetRepository).deleteByUsuarioId(10L);
        verify(resetRepository).save(any(TokenRestablecimientoPassword.class));
        verify(emailService).enviarEnlaceRestablecimiento(eq("user@test.com"), contains("http://app.test/reset/"));
    }

    @Test
    @DisplayName("Solicitar restablecimiento no hace nada si el usuario no existe")
    void solicitarRestablecimiento_noExiste_noEnviaCorreo() {
        when(usuarioRepository.findByEmail("noexiste@test.com")).thenReturn(Optional.empty());

        useCase.solicitarRestablecimiento(new SolicitarRestablecimientoPasswordDTO("noexiste@test.com"));

        verifyNoInteractions(resetRepository, emailService);
    }

    @Test
    @DisplayName("Restablecer contraseña lanza PasswordNoCoincideException si passwords no coinciden")
    void restablecerPassword_passwordsDistintas_lanzaExcepcion() {
        RestablecerPasswordDTO dto = new RestablecerPasswordDTO("token1", "Pass123!", "Pass456!");

        assertThatThrownBy(() -> useCase.restablecerPassword(dto))
                .isInstanceOf(PasswordNoCoincideException.class);
    }

    @Test
    @DisplayName("Restablecer contraseña lanza TokenRestablecimientoInvalidoException si token no existe o expiró")
    void restablecerPassword_tokenInvalido_lanzaExcepcion() {
        when(resetRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        RestablecerPasswordDTO dto = new RestablecerPasswordDTO("tokenInvalido", "Pass123!", "Pass123!");

        assertThatThrownBy(() -> useCase.restablecerPassword(dto))
                .isInstanceOf(TokenRestablecimientoInvalidoException.class);
    }

    @Test
    @DisplayName("Restablecer contraseña lanza TokenRestablecimientoInvalidoException si la cuenta está inactiva o bloqueada")
    void restablecerPassword_cuentaInactiva_lanzaExcepcion() {
        Usuario usuario = Usuario.builder().id(1L).email("u@test.com").nombre("U")
                .estado(EstadoUsuario.BLOQUEADO).activo(false).build();
        TokenRestablecimientoPassword token = new TokenRestablecimientoPassword(
                "hash", usuario, LocalDateTime.now().plusMinutes(30));

        when(resetRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        RestablecerPasswordDTO dto = new RestablecerPasswordDTO("tokenValido", "Pass123!", "Pass123!");

        assertThatThrownBy(() -> useCase.restablecerPassword(dto))
                .isInstanceOf(TokenRestablecimientoInvalidoException.class);
    }

    @Test
    @DisplayName("Restablece exitosamente la contraseña, activa cuenta si estaba pendiente y borra sesiones/intentos")
    void restablecerPassword_exitoso_cambiaPassword() {
        Usuario usuario = Usuario.builder().id(1L).email("u@test.com").nombre("U")
                .estado(EstadoUsuario.PENDIENTE_ACTIVACION).activo(true).build();
        TokenRestablecimientoPassword token = new TokenRestablecimientoPassword(
                "hash", usuario, LocalDateTime.now().plusMinutes(30));

        when(resetRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NuevaPass123!")).thenReturn("hashedPass");

        RestablecerPasswordDTO dto = new RestablecerPasswordDTO("tokenValido", "NuevaPass123!", "NuevaPass123!");
        useCase.restablecerPassword(dto);

        assertThat(usuario.getPassword()).isEqualTo("hashedPass");
        assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(token.getUsadoEn()).isNotNull();
        verify(usuarioRepository).save(usuario);
        verify(resetRepository).save(token);
        verify(sesionRepository).deleteByUsuarioId(1L);
        verify(intentoRepository).deleteByEmail("u@test.com");
    }
}
