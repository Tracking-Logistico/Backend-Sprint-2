package com.udea.demo.usuarios.application.dto;

import com.udea.demo.usuarios.domain.model.CanalNotificacion;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ActualizarPreferenciaRequestDTO - validación de canal y teléfono SMS")
class ActualizarPreferenciaRequestDTOTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @ParameterizedTest
    @EnumSource(CanalNotificacion.class)
    @DisplayName("Cada valor del enum CanalNotificacion es un canal válido")
    void canalDelDominio_valido(CanalNotificacion canal) {
        String telefonoSms = canal.incluyeSms() ? "+573001234567" : null;
        ActualizarPreferenciaRequestDTO dto = new ActualizarPreferenciaRequestDTO(canal, telefonoSms);

        Set<ConstraintViolation<ActualizarPreferenciaRequestDTO>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
        assertThat(dto.canal()).isEqualTo(canal);
        assertThat(dto.telefonoSms()).isEqualTo(telefonoSms);
    }

    @Test
    @DisplayName("Preferencia EMAIL sin teléfono SMS no produce violaciones")
    void preferenciaEmailValida_sinViolaciones() {
        ActualizarPreferenciaRequestDTO dto = new ActualizarPreferenciaRequestDTO(CanalNotificacion.EMAIL, null);

        assertThat(validator.validate(dto)).isEmpty();
        assertThat(dto.canal()).isEqualTo(CanalNotificacion.EMAIL);
        assertThat(dto.telefonoSms()).isNull();
    }

    @Test
    @DisplayName("Preferencia SMS con número internacional E.164 no produce violaciones")
    void preferenciaSmsValida_sinViolaciones() {
        ActualizarPreferenciaRequestDTO dto = new ActualizarPreferenciaRequestDTO(CanalNotificacion.SMS, "+573001234567");

        assertThat(validator.validate(dto)).isEmpty();
        assertThat(dto.canal()).isEqualTo(CanalNotificacion.SMS);
        assertThat(dto.telefonoSms()).isEqualTo("+573001234567");
    }

    @Test
    @DisplayName("Canal nulo produce violación @NotNull")
    void canalNulo_produceViolacion() {
        ActualizarPreferenciaRequestDTO dto = new ActualizarPreferenciaRequestDTO(null, "+573001234567");

        assertThat(validator.validate(dto)).anyMatch(v -> v.getPropertyPath().toString().equals("canal"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234", "telefono-invalido", "+012345678", "+57 300 1234567", ""})
    @DisplayName("Teléfono SMS con formato inválido produce violación @Pattern")
    void telefonoInvalido_produceViolacion(String telefono) {
        ActualizarPreferenciaRequestDTO dto = new ActualizarPreferenciaRequestDTO(CanalNotificacion.SMS, telefono);

        assertThat(validator.validate(dto)).anyMatch(v -> v.getPropertyPath().toString().equals("telefonoSms"));
    }

    @Test
    @DisplayName("equals, hashCode y toString operan sobre CanalNotificacion y el teléfono tipado")
    void equalsHashCodeToString() {
        ActualizarPreferenciaRequestDTO a = new ActualizarPreferenciaRequestDTO(CanalNotificacion.AMBOS, "+573104443322");
        ActualizarPreferenciaRequestDTO b = new ActualizarPreferenciaRequestDTO(CanalNotificacion.AMBOS, "+573104443322");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a.toString()).contains("AMBOS").contains("+573104443322");
    }
}
