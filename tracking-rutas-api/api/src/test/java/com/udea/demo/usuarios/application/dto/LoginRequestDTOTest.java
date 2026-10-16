package com.udea.demo.usuarios.application.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LoginRequestDTO - validación de credenciales")
class LoginRequestDTOTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("Email y contraseña reales no producen violaciones")
    void credencialesValidas_sinViolaciones() {
        LoginRequestDTO dto = new LoginRequestDTO("operador@tracking.com", "Password123!");

        Set<ConstraintViolation<LoginRequestDTO>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
        assertThat(dto.email()).isEqualTo("operador@tracking.com");
        assertThat(dto.password()).isEqualTo("Password123!");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "correo-sin-arroba", "usuario@", "@dominio.com"})
    @DisplayName("Email nulo, en blanco o mal formado produce violación")
    void emailInvalido(String email) {
        LoginRequestDTO dto = new LoginRequestDTO(email, "Password123!");

        assertThat(validator.validate(dto)).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Password nula o en blanco produce violación @NotBlank")
    void passwordEnBlanco(String password) {
        LoginRequestDTO dto = new LoginRequestDTO("usuario@tracking.com", password);

        assertThat(validator.validate(dto)).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }
}
