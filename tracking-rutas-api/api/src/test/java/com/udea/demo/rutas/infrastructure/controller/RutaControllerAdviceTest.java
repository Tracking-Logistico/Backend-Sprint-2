package com.udea.demo.rutas.infrastructure.controller;

import com.udea.demo.rutas.domain.exception.EnvioNoAsignadoException;
import com.udea.demo.rutas.domain.exception.EnvioYaAsignadoException;
import com.udea.demo.rutas.domain.exception.OrdenInvalidoException;
import com.udea.demo.rutas.domain.exception.RutaNoEncontradaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RutaControllerAdvice - mapeo de excepciones de rutas")
class RutaControllerAdviceTest {
    private final RutaControllerAdvice advice = new RutaControllerAdvice();

    @Test
    void rutaNoEncontradaEs404() {
        RutaNoEncontradaException ex = RutaNoEncontradaException.porId(100L);
        ResponseEntity<Map<String, String>> respuesta = advice.manejarRutaNoEncontrada(ex);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(respuesta.getBody()).containsEntry("error", ex.getMessage());
    }

    @Test
    void envioYaAsignadoEs409() {
        EnvioYaAsignadoException ex = new EnvioYaAsignadoException(10L);
        ResponseEntity<Map<String, String>> respuesta = advice.manejarEnvioYaAsignado(ex);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(respuesta.getBody()).containsEntry("pedidoId", ex.getMessage());
    }

    @Test
    void envioNoAsignadoEs409() {
        EnvioNoAsignadoException ex = new EnvioNoAsignadoException(10L);
        ResponseEntity<Map<String, String>> respuesta = advice.manejarEnvioNoAsignado(ex);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(respuesta.getBody()).containsEntry("pedidoId", ex.getMessage());
    }

    @Test
    void ordenInvalidoEs400() {
        OrdenInvalidoException ex = new OrdenInvalidoException();
        ResponseEntity<Map<String, String>> respuesta = advice.manejarOrdenInvalido(ex);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody()).containsEntry("orden", ex.getMessage());
    }
}
