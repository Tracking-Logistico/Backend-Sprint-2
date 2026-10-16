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

@DisplayName("RestablecerPasswordDTO - validación de token y nuevas credenciales")
class RestablecerPasswordDTOTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("Token y contraseñas no vacíos pasan la validación")
    void camposValidos_sinViolaciones() {
        RestablecerPasswordDTO dto = new RestablecerPasswordDTO(
                "token-restablecimiento", "PasswordSegura123!", "PasswordSegura123!");

        Set<ConstraintViolation<RestablecerPasswordDTO>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
        assertThat(dto.token()).isEqualTo("token-restablecimiento");
        assertThat(dto.nuevaPassword()).isEqualTo("PasswordSegura123!");
        assertThat(dto.confirmarPassword()).isEqualTo("PasswordSegura123!");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Token, nuevaPassword o confirmarPassword en blanco producen violación @NotBlank")
    void camposEnBlanco(String blanco) {
        assertThat(validator.validate(new RestablecerPasswordDTO(blanco, "PasswordSegura123!", "PasswordSegura123!")))
                .anyMatch(v -> v.getPropertyPath().toString().equals("token"));
        assertThat(validator.validate(new RestablecerPasswordDTO("token-restablecimiento", blanco, "PasswordSegura123!")))
                .anyMatch(v -> v.getPropertyPath().toString().equals("nuevaPassword"));
        assertThat(validator.validate(new RestablecerPasswordDTO("token-restablecimiento", "PasswordSegura123!", blanco)))
                .anyMatch(v -> v.getPropertyPath().toString().equals("confirmarPassword"));
    }
}
