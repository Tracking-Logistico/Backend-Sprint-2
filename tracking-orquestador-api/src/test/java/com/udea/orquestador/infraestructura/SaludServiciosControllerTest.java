package com.udea.orquestador.infraestructura;

import com.udea.orquestador.dominio.Servicio;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SaludServiciosControllerTest {

    @Test
    void estadoUpCuandoLosTresServiciosAceptanTcp() throws Exception {
        try (ServerSocket identidad = escuchar();
             ServerSocket envios = escuchar();
             ServerSocket rutas = escuchar()) {
            ConfiguracionServicios destinos = new ConfiguracionServicios(
                    uriDe(identidad), uriDe(envios), uriDe(rutas));
            SaludServiciosController controlador = new SaludServiciosController(destinos);

            ResponseEntity<Map<String, Object>> respuesta = controlador.estado();

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            Map<String, Object> cuerpo = respuesta.getBody();
            assertEquals("UP", cuerpo.get("estado"));
            @SuppressWarnings("unchecked")
            Map<String, String> servicios = (Map<String, String>) cuerpo.get("servicios");
            assertEquals("UP", servicios.get(Servicio.IDENTIDAD.name().toLowerCase()));
            assertEquals("UP", servicios.get(Servicio.ENVIOS.name().toLowerCase()));
            assertEquals("UP", servicios.get(Servicio.RUTAS.name().toLowerCase()));
        }
    }

    @Test
    void usaPuertoPorDefectoCuandoLaUriNoLoIncluye() {
        ConfiguracionServicios destinos = new ConfiguracionServicios(
                URI.create("http://identidad.invalid"),
                URI.create("https://envios.invalid"),
                URI.create("http://rutas.invalid:1"));
        SaludServiciosController controlador = new SaludServiciosController(destinos);

        ResponseEntity<Map<String, Object>> respuesta = controlador.estado();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, respuesta.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, String> servicios = (Map<String, String>) respuesta.getBody().get("servicios");
        assertEquals("DOWN", servicios.get(Servicio.IDENTIDAD.name().toLowerCase()));
        assertEquals("DOWN", servicios.get(Servicio.ENVIOS.name().toLowerCase()));
        assertEquals("DOWN", servicios.get(Servicio.RUTAS.name().toLowerCase()));
    }

    @Test
    void estadoDegradedCuandoAlgunServicioNoAceptaTcp() throws Exception {
        try (ServerSocket identidad = escuchar();
             ServerSocket envios = escuchar()) {
            URI rutasCaida = URI.create("http://127.0.0.1:1");
            ConfiguracionServicios destinos = new ConfiguracionServicios(
                    uriDe(identidad), uriDe(envios), rutasCaida);
            SaludServiciosController controlador = new SaludServiciosController(destinos);

            ResponseEntity<Map<String, Object>> respuesta = controlador.estado();

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, respuesta.getStatusCode());
            Map<String, Object> cuerpo = respuesta.getBody();
            assertEquals("DEGRADED", cuerpo.get("estado"));
            @SuppressWarnings("unchecked")
            Map<String, String> servicios = (Map<String, String>) cuerpo.get("servicios");
            assertEquals("UP", servicios.get(Servicio.IDENTIDAD.name().toLowerCase()));
            assertEquals("UP", servicios.get(Servicio.ENVIOS.name().toLowerCase()));
            assertEquals("DOWN", servicios.get(Servicio.RUTAS.name().toLowerCase()));
        }
    }

    private static ServerSocket escuchar() throws Exception {
        return new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
    }

    private static URI uriDe(ServerSocket socket) {
        return URI.create("http://127.0.0.1:" + socket.getLocalPort());
    }
}
