package com.udea.demo.usuarios.application.service;

import com.udea.demo.usuarios.application.dto.LoginResponseDTO;
import com.udea.demo.usuarios.application.dto.RefreshTokenRequestDTO;
import com.udea.demo.usuarios.domain.exception.SesionInvalidaException;
import com.udea.demo.usuarios.domain.model.EstadoUsuario;
import com.udea.demo.usuarios.domain.model.Rol;
import com.udea.demo.usuarios.domain.model.SesionUsuario;
import com.udea.demo.usuarios.domain.model.Usuario;
import com.udea.demo.usuarios.interfaces.persistence.SesionUsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GestionSesionesUseCase - creación, renovación y cierre de sesiones")
class GestionSesionesUseCaseTest {

    @Mock private SesionUsuarioRepository sesionRepository;
    @InjectMocks private GestionSesionesUseCase useCase;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(useCase, "diasRefresh", 7L);
        ReflectionTestUtils.setField(useCase, "minutosInactividadCliente", 30L);
        ReflectionTestUtils.setField(useCase, "minutosInactividadOperativo", 15L);
    }

    @Test
    @DisplayName("Crea sesión para cliente redirigiendo a su panel")
    void crearSesion_cliente_retornaRespuestaCorrecta() {
        Usuario usuario = Usuario.builder()
                .id(1L).email("cliente@test.com").rol(Rol.CLIENTE).estado(EstadoUsuario.ACTIVO).activo(true).build();

        LocalDateTime ahora = LocalDateTime.now();
        LoginResponseDTO res = useCase.crearSesion(usuario, ahora);

        assertThat(res.accessToken()).isNotBlank();
        assertThat(res.refreshToken()).isNotBlank();
        assertThat(res.rol()).isEqualTo(Rol.CLIENTE);
        assertThat(res.panel()).isEqualTo("/panel/cliente");
        assertThat(res.requiereCambioPassword()).isFalse();
        verify(sesionRepository).save(any(SesionUsuario.class));
    }

    @Test
    @DisplayName("Crea sesión para usuario pendiente de activación requiriendo cambio de contraseña")
    void crearSesion_pendienteActivacion_requiereCambio() {
        Usuario usuario = Usuario.builder()
                .id(2L).email("operador@test.com").rol(Rol.OPERADOR).estado(EstadoUsuario.PENDIENTE_ACTIVACION).activo(true).build();

        LocalDateTime ahora = LocalDateTime.now();
        LoginResponseDTO res = useCase.crearSesion(usuario, ahora);

        assertThat(res.requiereCambioPassword()).isTrue();
        assertThat(res.panel()).isEqualTo("/panel/configuracion");
    }

    @Test
    @DisplayName("Cierra sesión revocándola en el repositorio")
    void cerrarSesion_existente_revocaSesion() {
        SesionUsuario sesion = new SesionUsuario(
                Usuario.builder().id(1L).build(), "accHash", "refHash",
                LocalDateTime.now().plusHours(1), LocalDateTime.now().plusDays(7), LocalDateTime.now());

        when(sesionRepository.findByAccessTokenHash(anyString())).thenReturn(Optional.of(sesion));

        useCase.cerrarSesion("token123");

        assertThat(sesion.getRevokedAt()).isNotNull();
        verify(sesionRepository).save(sesion);
    }

    @Test
    @DisplayName("Cerrar sesión con token nulo o vacío no realiza acciones")
    void cerrarSesion_nulo_noHaceNada() {
        useCase.cerrarSesion(null);
        useCase.cerrarSesion("  ");
        verifyNoInteractions(sesionRepository);
    }

    @Test
    @DisplayName("Renovar sesión lanza SesionInvalidaException si el refresh token no existe")
    void renovarSesion_noExiste_lanzaExcepcion() {
        when(sesionRepository.findByRefreshTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.renovarSesion(new RefreshTokenRequestDTO("invalidToken")))
                .isInstanceOf(SesionInvalidaException.class);
    }

    @Test
    @DisplayName("Renovar sesión lanza SesionInvalidaException si la sesión ya fue revocada")
    void renovarSesion_revocada_lanzaExcepcion() {
        Usuario usuario = Usuario.builder().id(1L).rol(Rol.CLIENTE).estado(EstadoUsuario.ACTIVO).activo(true).build();
        SesionUsuario sesion = new SesionUsuario(usuario, "a", "r",
                LocalDateTime.now().plusHours(1), LocalDateTime.now().plusDays(7), LocalDateTime.now());
        sesion.setRevokedAt(LocalDateTime.now().minusMinutes(5));

        when(sesionRepository.findByRefreshTokenHash(anyString())).thenReturn(Optional.of(sesion));

        assertThatThrownBy(() -> useCase.renovarSesion(new RefreshTokenRequestDTO("token")))
                .isInstanceOf(SesionInvalidaException.class);
    }

    @Test
    @DisplayName("Renueva sesión válida revocando la anterior y devolviendo una nueva")
    void renovarSesion_valida_creaNuevaSesion() {
        Usuario usuario = Usuario.builder().id(3L).rol(Rol.CONDUCTOR).estado(EstadoUsuario.ACTIVO).activo(true).build();
        SesionUsuario sesion = new SesionUsuario(usuario, "acc", "ref",
                LocalDateTime.now().plusMinutes(10), LocalDateTime.now().plusDays(5), LocalDateTime.now());

        when(sesionRepository.findByRefreshTokenHash(anyString())).thenReturn(Optional.of(sesion));

        LoginResponseDTO res = useCase.renovarSesion(new RefreshTokenRequestDTO("validRefreshToken"));

        assertThat(sesion.getRevokedAt()).isNotNull();
        assertThat(res.accessToken()).isNotBlank();
        assertThat(res.panel()).isEqualTo("/panel/conductor");
    }
}
