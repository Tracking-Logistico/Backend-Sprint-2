package com.udea.orquestador.infraestructura;

import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EstadoProcesoControllerTest {

    @Test
    void healthDevuelveStatusUp() {
        EstadoProcesoController controlador = new EstadoProcesoController();

        Map<String, String> cuerpo = controlador.health();

        assertEquals(Map.of("status", "UP"), cuerpo);
    }
}
