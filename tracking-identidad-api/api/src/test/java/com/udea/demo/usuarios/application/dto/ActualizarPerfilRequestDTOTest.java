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

@DisplayName("ActualizarPerfilRequestDTO - validación de actualización de perfil")
class ActualizarPerfilRequestDTOTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    private ActualizarPerfilRequestDTO perfilValido() {
        return new ActualizarPerfilRequestDTO(
                "Ana María Gómez", "+573009998877", "Carrera 43A # 12-34, Medellín");
    }

    @Test
    @DisplayName("DTO con nombre, teléfono internacional y dirección reales no produce violaciones")
    void dtoValido_sinViolaciones() {
        ActualizarPerfilRequestDTO dto = perfilValido();

        Set<ConstraintViolation<ActualizarPerfilRequestDTO>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
        assertThat(dto.nombre()).isEqualTo("Ana María Gómez");
        assertThat(dto.telefono()).isEqualTo("+573009998877");
        assertThat(dto.direccion()).isEqualTo("Carrera 43A # 12-34, Medellín");
    }

    @Test
    @DisplayName("Teléfono y dirección nulos son opcionales y permitidos")
    void camposOpcionalesNulos_validos() {
        ActualizarPerfilRequestDTO dto = new ActualizarPerfilRequestDTO("Ana María Gómez", null, null);

        assertThat(validator.validate(dto)).isEmpty();
        assertThat(dto.telefono()).isNull();
        assertThat(dto.direccion()).isNull();
    }

    @Test
    @DisplayName("Teléfono vacío es válido según el patrón opcional del DTO")
    void telefonoVacio_valido() {
        ActualizarPerfilRequestDTO dto = new ActualizarPerfilRequestDTO("Ana María Gómez", "", "Carrera 43A # 12-34");

        assertThat(validator.validate(dto)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("Nombre en blanco produce violación @NotBlank")
    void nombreEnBlanco(String nombre) {
        ActualizarPerfilRequestDTO dto = new ActualizarPerfilRequestDTO(nombre, "+573001234567", "Calle 10 # 20-30");

        assertThat(validator.validate(dto))
                .anyMatch(v -> v.getPropertyPath().toString().equals("nombre"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234", "telefono-invalido", "+012345678", "+57 300 1234567"})
    @DisplayName("Teléfono con formato no internacional produce violación @Pattern")
    void telefonoInvalido(String telefono) {
        ActualizarPerfilRequestDTO dto = new ActualizarPerfilRequestDTO("Ana María Gómez", telefono, "Calle 10 # 20-30");

        assertThat(validator.validate(dto))
                .anyMatch(v -> v.getPropertyPath().toString().equals("telefono"));
    }

    @Test
    @DisplayName("Dirección mayor a 200 caracteres produce violación @Size")
    void direccionDemasiadoLarga() {
        String direccion = "Calle ".repeat(40);
        ActualizarPerfilRequestDTO dto = new ActualizarPerfilRequestDTO("Ana María Gómez", "+573001234567", direccion);

        assertThat(validator.validate(dto))
                .anyMatch(v -> v.getPropertyPath().toString().equals("direccion"));
    }

    @Test
    @DisplayName("equals, hashCode y toString operan sobre los campos tipados del record")
    void equalsHashCodeToString() {
        ActualizarPerfilRequestDTO a = perfilValido();
        ActualizarPerfilRequestDTO b = perfilValido();
        ActualizarPerfilRequestDTO c = new ActualizarPerfilRequestDTO(
                "Diana Restrepo", "+573105554433", "Avenida El Poblado # 10-20");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a).isNotEqualTo(c);
        assertThat(a.toString()).contains("Ana María Gómez").contains("+573009998877");
    }
}
