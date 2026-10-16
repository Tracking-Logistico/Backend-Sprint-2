package com.udea.demo.usuarios.application.dto;

import com.udea.demo.usuarios.domain.model.Rol;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LoginResponseDTO - estructura con tokens, rol y flags de sesión")
class LoginResponseDTOTest {

    @Test
    @DisplayName("El constructor canónico asigna tokens, expiraciones, Rol, panel, usuarioId y flag de password")
    void crearInstancia_asignaCamposTipados() {
        LocalDateTime accesoExpira = LocalDateTime.of(2026, 10, 16, 9, 0);
        LocalDateTime refreshExpira = LocalDateTime.of(2026, 10, 23, 8, 45);

        LoginResponseDTO dto = new LoginResponseDTO(
                "access-token-sesion", "refresh-token-sesion",
                accesoExpira, refreshExpira, Rol.OPERADOR, "/panel/operador",
                99L, true);

        assertThat(dto.accessToken()).isEqualTo("access-token-sesion");
        assertThat(dto.refreshToken()).isEqualTo("refresh-token-sesion");
        assertThat(dto.accessTokenExpiresAt()).isEqualTo(accesoExpira);
        assertThat(dto.refreshTokenExpiresAt()).isEqualTo(refreshExpira);
        assertThat(dto.rol()).isEqualTo(Rol.OPERADOR);
        assertThat(dto.panel()).isEqualTo("/panel/operador");
        assertThat(dto.usuarioId()).isEqualTo(99L);
        assertThat(dto.requiereCambioPassword()).isTrue();
    }

    @Test
    @DisplayName("El constructor compacto deja usuarioId nulo y requiereCambioPassword en false")
    void constructorCompacto_valoresPorDefecto() {
        LocalDateTime ahora = LocalDateTime.of(2026, 10, 16, 8, 45);
        LoginResponseDTO dto = new LoginResponseDTO(
                "access-token-sesion", "refresh-token-sesion",
                ahora, ahora.plusDays(7), Rol.CLIENTE, "/panel/cliente");

        assertThat(dto.usuarioId()).isNull();
        assertThat(dto.requiereCambioPassword()).isFalse();
        assertThat(dto.rol()).isEqualTo(Rol.CLIENTE);
    }
}
