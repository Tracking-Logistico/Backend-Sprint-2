package com.udea.demo.usuarios.application.service;

import com.udea.demo.usuarios.domain.exception.PasswordDebilException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PasswordPolicyService - validación de robustez de contraseñas")
class PasswordPolicyServiceTest {

    private final PasswordPolicyService service = new PasswordPolicyService();

    @Test
    @DisplayName("Acepta contraseña robusta con mayúscula, minúscula, dígito y símbolo")
    void validar_passwordRobusta_pasa() {
        assertThatNoException().isThrownBy(() ->
                service.validar("Segura123!@", "usuario@test.com", "Carlos"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"short", "nocaps123!", "NOLOWER123!", "NoSpecial123", "NoDigit!@#"})
    @DisplayName("Rechaza contraseñas que no cumplen complejidad")
    void validar_sinComplejidad_lanzaExcepcion(String debil) {
        assertThatThrownBy(() -> service.validar(debil, "usuario@test.com", "Carlos"))
                .isInstanceOf(PasswordDebilException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345678", "password", "password1", "qwerty123", "admin123"})
    @DisplayName("Rechaza contraseñas comunes del diccionario")
    void validar_comunes_lanzaExcepcion(String comun) {
        assertThatThrownBy(() -> service.validar(comun, "usuario@test.com", "Carlos"))
                .isInstanceOf(PasswordDebilException.class);
    }

    @Test
    @DisplayName("Rechaza contraseñas iguales al correo del usuario")
    void validar_igualACorreo_lanzaExcepcion() {
        assertThatThrownBy(() -> service.validar("Usuario123!", "usuario123!", "Carlos"))
                .isInstanceOf(PasswordDebilException.class);
    }

    @Test
    @DisplayName("Rechaza contraseñas iguales al nombre del usuario")
    void validar_igualANombre_lanzaExcepcion() {
        assertThatThrownBy(() -> service.validar("Carlos123!", "u@test.com", "carlos123!"))
                .isInstanceOf(PasswordDebilException.class);
    }
}
