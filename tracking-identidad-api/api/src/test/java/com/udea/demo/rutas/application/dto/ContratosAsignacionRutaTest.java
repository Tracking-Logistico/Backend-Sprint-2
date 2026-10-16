package com.udea.demo.rutas.application.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DTOs de asignación de ruta - validación con tipos Long y List<Long>")
class ContratosAsignacionRutaTest {
    private static Validator validator;

    @BeforeAll
    static void validador() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void asignarEnvioExigeIdentificadoresPositivos() {
        Long pedidoId = 10L;
        Long conductorId = 5L;
        assertThat(validator.validate(new AsignarEnvioRequestDTO(pedidoId, conductorId))).isEmpty();
        assertThat(validator.validate(new AsignarEnvioRequestDTO(null, conductorId))).isNotEmpty();
        assertThat(validator.validate(new AsignarEnvioRequestDTO(pedidoId, 0L))).isNotEmpty();
    }

    @Test
    void reasignarExigePedidoYNuevoConductor() {
        Long pedidoId = 10L;
        Long nuevoConductorId = 6L;
        ReasignarEnvioRequestDTO dto = new ReasignarEnvioRequestDTO(pedidoId, nuevoConductorId, "Cambio de zona");
        assertThat(validator.validate(dto)).isEmpty();
        assertThat(dto.pedidoId()).isEqualTo(pedidoId);
        assertThat(dto.nuevoConductorId()).isEqualTo(nuevoConductorId);
        assertThat(validator.validate(new ReasignarEnvioRequestDTO(null, nuevoConductorId))).isNotEmpty();
    }

    @Test
    void reordenarExigeListaDePedidos() {
        List<Long> orden = List.of(10L, 20L, 30L);
        assertThat(validator.validate(new ReordenarRutaRequestDTO(orden))).isEmpty();
        assertThat(validator.validate(new ReordenarRutaRequestDTO(List.of()))).isNotEmpty();
    }

    @Test
    void asignacionMasivaExigeConductorYPedidos() {
        Long conductorId = 5L;
        List<Long> pedidos = List.of(10L, 20L);
        assertThat(validator.validate(new AsignacionMasivaRequestDTO(conductorId, pedidos))).isEmpty();
        assertThat(validator.validate(new AsignacionMasivaRequestDTO(0L, List.of()))).isNotEmpty();
    }
}
