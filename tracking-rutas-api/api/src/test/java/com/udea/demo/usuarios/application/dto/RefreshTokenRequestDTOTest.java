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

@DisplayName("RefreshTokenRequestDTO - validación del token de renovación")
class RefreshTokenRequestDTOTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("Refresh token no vacío no genera violaciones")
    void tokenValido_sinViolaciones() {
        RefreshTokenRequestDTO dto = new RefreshTokenRequestDTO("refresh-token-sesion");

        Set<ConstraintViolation<RefreshTokenRequestDTO>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
        assertThat(dto.refreshToken()).isEqualTo("refresh-token-sesion");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Refresh token nulo o en blanco produce violación @NotBlank")
    void refreshTokenEnBlanco(String token) {
        RefreshTokenRequestDTO dto = new RefreshTokenRequestDTO(token);

        assertThat(validator.validate(dto)).anyMatch(v -> v.getPropertyPath().toString().equals("refreshToken"));
    }
}
