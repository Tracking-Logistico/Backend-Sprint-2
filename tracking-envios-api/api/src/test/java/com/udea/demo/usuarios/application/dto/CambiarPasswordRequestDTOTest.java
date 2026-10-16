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

@DisplayName("CambiarPasswordRequestDTO - validación de política de contraseña")
class CambiarPasswordRequestDTOTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("Contraseña actual y nueva que cumplen complejidad no producen violaciones")
    void dtoValido_sinViolaciones() {
        CambiarPasswordRequestDTO dto = new CambiarPasswordRequestDTO(
                "Temporal1!", "NuevaClave1!", "NuevaClave1!");

        Set<ConstraintViolation<CambiarPasswordRequestDTO>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
        assertThat(dto.passwordActual()).isEqualTo("Temporal1!");
        assertThat(dto.nuevaPassword()).isEqualTo("NuevaClave1!");
        assertThat(dto.confirmarPassword()).isEqualTo("NuevaClave1!");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("passwordActual en blanco produce violación")
    void passwordActualEnBlanco(String actual) {
        CambiarPasswordRequestDTO dto = new CambiarPasswordRequestDTO(actual, "NuevaClave1!", "NuevaClave1!");

        assertThat(validator.validate(dto)).anyMatch(v -> v.getPropertyPath().toString().equals("passwordActual"));
    }

    @Test
    @DisplayName("nuevaPassword corta produce violación @Size")
    void nuevaPasswordCorta() {
        CambiarPasswordRequestDTO dto = new CambiarPasswordRequestDTO("Actual1!", "Ab1!", "Ab1!");

        assertThat(validator.validate(dto)).anyMatch(v -> v.getPropertyPath().toString().equals("nuevaPassword"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"sinmayus1!", "SINMINUS1!", "SinNumero!!", "SinEspecial1"})
    @DisplayName("nuevaPassword sin complejidad produce violación @Pattern")
    void nuevaPasswordSinComplejidad(String nueva) {
        CambiarPasswordRequestDTO dto = new CambiarPasswordRequestDTO("Actual1!", nueva, nueva);

        assertThat(validator.validate(dto)).anyMatch(v -> v.getPropertyPath().toString().equals("nuevaPassword"));
    }
}
