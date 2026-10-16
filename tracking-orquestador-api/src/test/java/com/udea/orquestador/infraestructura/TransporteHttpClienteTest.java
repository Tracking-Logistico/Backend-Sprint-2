package com.udea.orquestador.infraestructura;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransporteHttpClienteTest {

    @Test
    void reenviaMetodoRutaConsultaAutorizacionCuerpoYCabecerasDeRespuesta() throws Exception {
        byte[] cuerpoEntrada = "{\"detalle\":\"prueba\"}".getBytes(StandardCharsets.UTF_8);
        byte[] cuerpoSalida = new byte[]{37, 80, 68, 70};
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/api/v1/pedidos/12/trazabilidad", intercambio -> {
            assertEquals("POST", intercambio.getRequestMethod());
            assertEquals("tipo=INCIDENCIA", intercambio.getRequestURI().getRawQuery());
            assertEquals("Bearer token-de-prueba", intercambio.getRequestHeaders().getFirst("Authorization"));
            assertEquals(new String(cuerpoEntrada, StandardCharsets.UTF_8),
                    new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            intercambio.getResponseHeaders().add("Content-Type", "application/pdf");
            intercambio.getResponseHeaders().add("Content-Disposition", "attachment; filename=historial.pdf");
            intercambio.sendResponseHeaders(201, cuerpoSalida.length);
            try (var salida = intercambio.getResponseBody()) {
                salida.write(cuerpoSalida);
            }
        });
        servidor.start();
        try {
            MockHttpServletRequest entrada = new MockHttpServletRequest("POST", "/api/v1/pedidos/12/trazabilidad");
            entrada.setQueryString("tipo=INCIDENCIA");
            entrada.addHeader("Authorization", "Bearer token-de-prueba");
            entrada.setContent(cuerpoEntrada);
            MockHttpServletResponse salida = new MockHttpServletResponse();

            new TransporteHttpCliente(1000, 2000).reenviar(base(servidor), entrada, salida);

            assertEquals(201, salida.getStatus());
            assertArrayEquals(cuerpoSalida, salida.getContentAsByteArray());
            assertEquals("attachment; filename=historial.pdf", salida.getHeader("Content-Disposition"));
        } finally {
            servidor.stop(0);
        }
    }

    @Test
    void noReenviaOriginNiCabecerasHopByHopPeroSiAutorizacionYForwarded() throws Exception {
        AtomicReference<String> origin = new AtomicReference<>();
        AtomicReference<String> connection = new AtomicReference<>();
        AtomicReference<String> forwardedFor = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/api/v1/clientes/registro", intercambio -> {
            origin.set(intercambio.getRequestHeaders().getFirst("Origin"));
            connection.set(intercambio.getRequestHeaders().getFirst("Connection"));
            forwardedFor.set(intercambio.getRequestHeaders().getFirst("X-Forwarded-For"));
            authorization.set(intercambio.getRequestHeaders().getFirst("Authorization"));
            intercambio.getRequestBody().readAllBytes();
            byte[] datos = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(201, datos.length);
            try (var salida = intercambio.getResponseBody()) {
                salida.write(datos);
            }
        });
        servidor.start();
        try {
            MockHttpServletRequest entrada = new MockHttpServletRequest("POST", "/api/v1/clientes/registro");
            entrada.setRemoteAddr("10.1.2.3");
            entrada.setScheme("https");
            entrada.addHeader("Host", "gateway.local");
            entrada.addHeader("Origin", "https://logistrack-frontend-demo.onrender.com");
            entrada.addHeader("Connection", "keep-alive");
            entrada.addHeader("X-Forwarded-For", "1.1.1.1");
            entrada.addHeader("Authorization", "Bearer token-de-prueba");
            entrada.addHeader("Content-Type", "application/json");
            entrada.setContent("{\"nombre\":\"Prueba\"}".getBytes(StandardCharsets.UTF_8));
            MockHttpServletResponse salida = new MockHttpServletResponse();

            new TransporteHttpCliente(1000, 2000).reenviar(base(servidor), entrada, salida);

            assertEquals(201, salida.getStatus());
            assertNull(origin.get());
            assertNull(connection.get());
            assertEquals("10.1.2.3", forwardedFor.get());
            assertEquals("Bearer token-de-prueba", authorization.get());
        } finally {
            servidor.stop(0);
        }
    }

    @Test
    void noCopiaCabecerasCorsDelBackend() throws Exception {
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/api/v1/auth/login", intercambio -> {
            intercambio.getRequestBody().readAllBytes();
            intercambio.getResponseHeaders().add("Access-Control-Allow-Origin", "https://backend.example");
            intercambio.getResponseHeaders().add("Access-Control-Allow-Credentials", "true");
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            byte[] datos = "{}".getBytes(StandardCharsets.UTF_8);
            intercambio.sendResponseHeaders(200, datos.length);
            try (var salida = intercambio.getResponseBody()) {
                salida.write(datos);
            }
        });
        servidor.start();
        try {
            MockHttpServletRequest entrada = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            entrada.setContent("{}".getBytes(StandardCharsets.UTF_8));
            MockHttpServletResponse salida = new MockHttpServletResponse();

            new TransporteHttpCliente(1000, 2000).reenviar(base(servidor), entrada, salida);

            assertEquals(200, salida.getStatus());
            assertNull(salida.getHeader("Access-Control-Allow-Origin"));
            assertNull(salida.getHeader("Access-Control-Allow-Credentials"));
            assertEquals("application/json", salida.getHeader("Content-Type"));
        } finally {
            servidor.stop(0);
        }
    }

    @Test
    void getNoEnviaCuerpoYHeadNoCopiaElCuerpoDeRespuesta() throws Exception {
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/api/v1/pedidos", intercambio -> {
            byte[] leido = intercambio.getRequestBody().readAllBytes();
            assertEquals(0, leido.length);
            byte[] datos = "cuerpo".getBytes(StandardCharsets.UTF_8);
            intercambio.sendResponseHeaders(200, datos.length);
            try (var salida = intercambio.getResponseBody()) {
                salida.write(datos);
            }
        });
        servidor.start();
        try {
            TransporteHttpCliente transporte = new TransporteHttpCliente(1000, 2000);

            MockHttpServletRequest get = new MockHttpServletRequest("GET", "/api/v1/pedidos");
            MockHttpServletResponse salidaGet = new MockHttpServletResponse();
            transporte.reenviar(base(servidor), get, salidaGet);
            assertEquals(200, salidaGet.getStatus());
            assertEquals("cuerpo", salidaGet.getContentAsString());

            MockHttpServletRequest head = new MockHttpServletRequest("HEAD", "/api/v1/pedidos");
            MockHttpServletResponse salidaHead = new MockHttpServletResponse();
            transporte.reenviar(base(servidor), head, salidaHead);
            assertEquals(200, salidaHead.getStatus());
            assertEquals(0, salidaHead.getContentAsByteArray().length);
        } finally {
            servidor.stop(0);
        }
    }

    @Test
    void deleteConContentLengthCeroNoEnviaCuerpo() throws Exception {
        AtomicReference<Integer> bytesLeidos = new AtomicReference<>();
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/api/v1/usuarios/1", intercambio -> {
            bytesLeidos.set(intercambio.getRequestBody().readAllBytes().length);
            intercambio.sendResponseHeaders(204, -1);
        });
        servidor.start();
        try {
            MockHttpServletRequest entrada = new MockHttpServletRequest("DELETE", "/api/v1/usuarios/1");
            entrada.setContent(new byte[0]);
            MockHttpServletResponse salida = new MockHttpServletResponse();

            new TransporteHttpCliente(1000, 2000).reenviar(base(servidor), entrada, salida);

            assertEquals(204, salida.getStatus());
            assertEquals(0, bytesLeidos.get());
        } finally {
            servidor.stop(0);
        }
    }

    @Test
    void deleteSinLongitudDeCuerpoCeroSiEnviaInputStream() throws Exception {
        AtomicReference<Integer> bytesLeidos = new AtomicReference<>();
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/api/v1/usuarios/1", intercambio -> {
            bytesLeidos.set(intercambio.getRequestBody().readAllBytes().length);
            intercambio.sendResponseHeaders(204, -1);
        });
        servidor.start();
        try {
            MockHttpServletRequest entrada = new MockHttpServletRequest("DELETE", "/api/v1/usuarios/1");
            MockHttpServletResponse salida = new MockHttpServletResponse();

            new TransporteHttpCliente(1000, 2000).reenviar(base(servidor), entrada, salida);

            assertEquals(204, salida.getStatus());
            assertEquals(0, bytesLeidos.get());
        } finally {
            servidor.stop(0);
        }
    }

    @Test
    void responde502CuandoElServicioNoEstaDisponible() throws Exception {
        MockHttpServletRequest entrada = new MockHttpServletRequest("GET", "/api/v1/pedidos");
        MockHttpServletResponse salida = new MockHttpServletResponse();
        URI caido = URI.create("http://127.0.0.1:1");

        new TransporteHttpCliente(200, 500).reenviar(caido, entrada, salida);

        assertEquals(502, salida.getStatus());
        assertTrue(salida.getContentAsString().contains("SERVICIO_NO_DISPONIBLE"));
        assertEquals("application/json;charset=UTF-8", salida.getContentType());
    }

    @Test
    void responde504CuandoElServicioExcedeElTiempo() throws Exception {
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/api/v1/pedidos", intercambio -> {
            try {
                Thread.sleep(400);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            intercambio.sendResponseHeaders(200, 0);
            intercambio.close();
        });
        servidor.start();
        try {
            MockHttpServletRequest entrada = new MockHttpServletRequest("GET", "/api/v1/pedidos");
            MockHttpServletResponse salida = new MockHttpServletResponse();

            new TransporteHttpCliente(1000, 80).reenviar(base(servidor), entrada, salida);

            assertEquals(504, salida.getStatus());
            assertTrue(salida.getContentAsString().contains("TIMEOUT_SERVICIO"));
        } finally {
            servidor.stop(0);
        }
    }

    @Test
    void cabeceraInvalidaSePropagaAntesDeContactarElServicio() {
        MockHttpServletRequest entrada = new MockHttpServletRequest("GET", "/api/v1/pedidos");
        entrada.addHeader("Not Valid", "valor");
        MockHttpServletResponse salida = new MockHttpServletResponse();
        URI destino = URI.create("http://127.0.0.1:65535");

        assertThrows(IllegalArgumentException.class,
                () -> new TransporteHttpCliente(200, 500).reenviar(destino, entrada, salida));
        assertEquals(200, salida.getStatus());
    }

    private static URI base(HttpServer servidor) {
        return URI.create("http://127.0.0.1:" + servidor.getAddress().getPort());
    }
}
