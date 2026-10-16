package com.udea.demo.usuarios.application.dto;

import com.udea.demo.usuarios.domain.model.Rol;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ActualizarRolUsuarioRequestDTO - validación de rol interno")
class ActualizarRolUsuarioRequestDTOTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @ParameterizedTest
    @EnumSource(value = Rol.class, names = {"OPERADOR", "CONDUCTOR"})
    @DisplayName("Roles internos del dominio son aceptados")
    void rolesInternosValidos(Rol rol) {
        String licencia = rol == Rol.CONDUCTOR ? "LIC-B2-998877" : null;
        String codigoEmpleado = rol == Rol.OPERADOR ? "OP-001" : null;
        ActualizarRolUsuarioRequestDTO dto = new ActualizarRolUsuarioRequestDTO(rol, licencia, codigoEmpleado);

        Set<ConstraintViolation<ActualizarRolUsuarioRequestDTO>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
        assertThat(dto.rol()).isEqualTo(rol);
        assertThat(dto.licencia()).isEqualTo(licencia);
        assertThat(dto.codigoEmpleado()).isEqualTo(codigoEmpleado);
    }

    @Test
    @DisplayName("Rol nulo produce violación @NotNull")
    void rolNulo() {
        ActualizarRolUsuarioRequestDTO dto = new ActualizarRolUsuarioRequestDTO(null, null, "OP-001");

        assertThat(validator.validate(dto)).anyMatch(v -> v.getPropertyPath().toString().equals("rol"));
    }
}
