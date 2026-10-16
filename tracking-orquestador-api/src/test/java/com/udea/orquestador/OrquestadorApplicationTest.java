package com.udea.orquestador;

import com.udea.orquestador.aplicacion.Enrutador;
import com.udea.orquestador.infraestructura.ConfiguracionServicios;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@TestPropertySource(properties = {
        "upstreams.identidad=http://localhost:8081",
        "upstreams.envios=http://localhost:8082",
        "upstreams.rutas=http://localhost:8083",
        "server.port=0"
})
class OrquestadorApplicationTest {

    @Autowired
    private Enrutador enrutador;
    @Autowired
    private ConfiguracionServicios servicios;

    @Test
    void contextoCargaLosBeansDelOrquestador() {
        assertNotNull(enrutador);
        assertNotNull(servicios);
    }
}
