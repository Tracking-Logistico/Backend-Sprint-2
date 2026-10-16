package com.udea.orquestador.infraestructura;

import com.udea.orquestador.dominio.Servicio;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfiguracionServiciosTest {

    @Test
    void guardaUrisHttpYHttpsPorServicio() {
        URI identidad = URI.create("http://localhost:8081");
        URI envios = URI.create("http://localhost:8082");
        URI rutas = URI.create("https://localhost:8083");

        ConfiguracionServicios configuracion = new ConfiguracionServicios(identidad, envios, rutas);

        assertEquals(identidad, configuracion.url(Servicio.IDENTIDAD));
        assertEquals(envios, configuracion.url(Servicio.ENVIOS));
        assertEquals(rutas, configuracion.url(Servicio.RUTAS));
    }

    @Test
    void aceptaRutaRaizComoUrlInterna() {
        URI identidad = URI.create("http://localhost:8081/");
        URI envios = URI.create("http://localhost:8082");
        URI rutas = URI.create("http://localhost:8083");

        ConfiguracionServicios configuracion = new ConfiguracionServicios(identidad, envios, rutas);

        assertEquals(identidad, configuracion.url(Servicio.IDENTIDAD));
    }

    @Test
    void rechazaUriHttpOpacaSinHost() {
        URI sinHost = URI.create("http:identidad");
        URI envios = URI.create("http://localhost:8082");
        URI rutas = URI.create("http://localhost:8083");

        assertThrows(IllegalArgumentException.class,
                () -> new ConfiguracionServicios(sinHost, envios, rutas));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ftp://localhost:8081",
            "http://localhost:8081/api",
            "http://usuario:clave@localhost:8081",
            "http://localhost:8081?q=1",
            "http://localhost:8081#frag"
    })
    void rechazaUriInternaInvalida(String uriInvalida) {
        URI identidad = URI.create(uriInvalida);
        URI envios = URI.create("http://localhost:8082");
        URI rutas = URI.create("http://localhost:8083");

        assertThrows(IllegalArgumentException.class,
                () -> new ConfiguracionServicios(identidad, envios, rutas));
    }
}
