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

@DisplayName("SolicitarRestablecimientoPasswordDTO - validación de email")
class SolicitarRestablecimientoPasswordDTOTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("Email de cliente válido no produce violaciones")
    void emailValido_sinViolaciones() {
        SolicitarRestablecimientoPasswordDTO dto =
                new SolicitarRestablecimientoPasswordDTO("ana.gomez@tracking.com");

        Set<ConstraintViolation<SolicitarRestablecimientoPasswordDTO>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
        assertThat(dto.email()).isEqualTo("ana.gomez@tracking.com");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "sin-arroba", "correo@", "@servidor.com"})
    @DisplayName("Email nulo, en blanco o mal formado produce violación")
    void emailInvalido(String email) {
        SolicitarRestablecimientoPasswordDTO dto = new SolicitarRestablecimientoPasswordDTO(email);

        assertThat(validator.validate(dto)).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }
}
