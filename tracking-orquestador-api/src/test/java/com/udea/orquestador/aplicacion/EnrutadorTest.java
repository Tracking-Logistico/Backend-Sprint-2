package com.udea.orquestador.aplicacion;

import com.udea.orquestador.dominio.Servicio;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EnrutadorTest {
    private final Enrutador enrutador = new Enrutador();

    @Test void conservaEndpointsOriginales() {
        assertEquals(Servicio.IDENTIDAD, enrutador.resolver("/api/v1/auth/login").orElseThrow());
        assertEquals(Servicio.IDENTIDAD, enrutador.resolver("/api/v1/notificaciones/preferencias").orElseThrow());
        assertEquals(Servicio.ENVIOS, enrutador.resolver("/api/v1/pedidos/24/trazabilidad/exportar").orElseThrow());
        assertEquals(Servicio.ENVIOS, enrutador.resolver("/api/v1/pedidos/mios").orElseThrow());
        assertEquals(Servicio.RUTAS, enrutador.resolver("/api/v1/rutas/mi-ruta").orElseThrow());
        assertEquals(Servicio.RUTAS, enrutador.resolver("/api/v1/conductor/panel/entregas").orElseThrow());
    }

    @Test void noAceptaPrefijosParecidosONuevasRutas() {
        assertTrue(enrutador.resolver("/api/v1/pedidos-extra").isEmpty());
        assertTrue(enrutador.resolver("/api/v1/otra-ruta").isEmpty());
        assertTrue(enrutador.resolver("/internal/admin").isEmpty());
    }
}
