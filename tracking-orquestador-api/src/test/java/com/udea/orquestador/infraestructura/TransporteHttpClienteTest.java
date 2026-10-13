package com.udea.orquestador.infraestructura;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.*;

class TransporteHttpClienteTest {
    @Test void mantieneMetodoRutaConsultaTokenCuerpoYRespuesta() throws Exception {
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/api/v1/pedidos/12/trazabilidad", intercambio -> {
            assertEquals("POST", intercambio.getRequestMethod());
            assertEquals("tipo=INCIDENCIA", intercambio.getRequestURI().getRawQuery());
            assertEquals("Bearer token-de-prueba", intercambio.getRequestHeaders().getFirst("Authorization"));
            assertEquals("{\"detalle\":\"prueba\"}", new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            intercambio.getResponseHeaders().add("Content-Type", "application/pdf");
            intercambio.getResponseHeaders().add("Content-Disposition", "attachment; filename=historial.pdf");
            byte[] datos = new byte[]{37,80,68,70};
            intercambio.sendResponseHeaders(201, datos.length);
            try (var salida = intercambio.getResponseBody()) { salida.write(datos); }
        });
        servidor.start();
        try {
            MockHttpServletRequest entrada = new MockHttpServletRequest("POST", "/api/v1/pedidos/12/trazabilidad");
            entrada.setQueryString("tipo=INCIDENCIA");
            entrada.addHeader("Authorization", "Bearer token-de-prueba");
            entrada.setContent("{\"detalle\":\"prueba\"}".getBytes(StandardCharsets.UTF_8));
            MockHttpServletResponse salida = new MockHttpServletResponse();
            new TransporteHttpCliente(1000, 2000).reenviar(
                    URI.create("http://127.0.0.1:" + servidor.getAddress().getPort()), entrada, salida);
            assertEquals(201, salida.getStatus());
            assertArrayEquals(new byte[]{37,80,68,70}, salida.getContentAsByteArray());
            assertEquals("attachment; filename=historial.pdf", salida.getHeader("Content-Disposition"));
        } finally { servidor.stop(0); }
    }
}
