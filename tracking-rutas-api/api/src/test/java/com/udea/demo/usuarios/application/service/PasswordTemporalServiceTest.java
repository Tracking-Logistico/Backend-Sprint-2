package com.udea.demo.usuarios.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PasswordTemporalService - generación de contraseñas temporales")
class PasswordTemporalServiceTest {

    private final PasswordTemporalService service = new PasswordTemporalService();
    private final PasswordPolicyService policyService = new PasswordPolicyService();

    @RepeatedTest(10)
    @DisplayName("Genera contraseñas temporales de 16 caracteres que cumplen la política de robustez")
    void generar_cumplePoliticaYLongitud() {
        String temporal = service.generar();

        assertThat(temporal).isNotNull();
        assertThat(temporal).hasSize(16);
        // Validar que cumple mayúscula, minúscula, número y símbolo
        policyService.validar(temporal, "otro@test.com", "Otro Nombre");
    }
}
