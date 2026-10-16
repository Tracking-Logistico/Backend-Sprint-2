package com.udea.demo.usuarios.application.dto;

import com.udea.demo.usuarios.domain.model.Rol;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EditarUsuarioInternoCommand - validación de edición de usuario interno")
class EditarUsuarioInternoCommandTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("Edición de conductor con Rol.CONDUCTOR, teléfono y licencia no produce violaciones")
    void edicionConductorValida() {
        EditarUsuarioInternoCommand cmd = new EditarUsuarioInternoCommand(
                "Julián Castaño", "3002223344", "Calle 33 # 65-12, Medellín",
                Rol.CONDUCTOR, "LIC-B2-998877", null);

        Set<ConstraintViolation<EditarUsuarioInternoCommand>> violations = validator.validate(cmd);

        assertThat(violations).isEmpty();
        assertThat(cmd.rol()).isEqualTo(Rol.CONDUCTOR);
        assertThat(cmd.licencia()).isEqualTo("LIC-B2-998877");
    }

    @Test
    @DisplayName("Teléfono con letras produce violación @Pattern")
    void telefonoInvalido() {
        EditarUsuarioInternoCommand cmd = new EditarUsuarioInternoCommand(
                "Laura Ops", "abc", "Calle 10 # 40-20", Rol.OPERADOR, null, "OP-001");

        assertThat(validator.validate(cmd)).anyMatch(v -> v.getPropertyPath().toString().equals("telefono"));
    }
}
