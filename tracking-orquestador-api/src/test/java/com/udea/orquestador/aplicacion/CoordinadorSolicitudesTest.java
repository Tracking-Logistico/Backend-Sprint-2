package com.udea.orquestador.aplicacion;

import com.udea.orquestador.dominio.Servicio;
import com.udea.orquestador.infraestructura.ConfiguracionServicios;
import java.net.URI;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoordinadorSolicitudesTest {

    @Mock
    private Enrutador enrutador;
    @Mock
    private TransporteHttp transporte;
    @Mock
    private ConfiguracionServicios servicios;
    @InjectMocks
    private CoordinadorSolicitudes coordinador;

    @Test
    void ejecutaReenvioConLaUriDelServicioResuelto() throws Exception {
        MockHttpServletRequest entrada = new MockHttpServletRequest("GET", "/api/v1/pedidos");
        MockHttpServletResponse salida = new MockHttpServletResponse();
        URI destino = URI.create("http://localhost:8082");

        when(enrutador.resolver("/api/v1/pedidos")).thenReturn(Optional.of(Servicio.ENVIOS));
        when(servicios.url(Servicio.ENVIOS)).thenReturn(destino);

        coordinador.ejecutar(entrada, salida);

        verify(transporte).reenviar(destino, entrada, salida);
        verify(servicios).url(Servicio.ENVIOS);
    }

    @Test
    void ejecutaReenvioHaciaIdentidad() throws Exception {
        MockHttpServletRequest entrada = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        MockHttpServletResponse salida = new MockHttpServletResponse();
        URI destino = URI.create("http://localhost:8081");

        when(enrutador.resolver("/api/v1/auth/login")).thenReturn(Optional.of(Servicio.IDENTIDAD));
        when(servicios.url(Servicio.IDENTIDAD)).thenReturn(destino);

        coordinador.ejecutar(entrada, salida);

        verify(transporte).reenviar(destino, entrada, salida);
    }

    @Test
    void responde404CuandoNoHayServicio() throws Exception {
        MockHttpServletRequest entrada = new MockHttpServletRequest("GET", "/api/v1/desconocido");
        MockHttpServletResponse salida = new MockHttpServletResponse();

        when(enrutador.resolver("/api/v1/desconocido")).thenReturn(Optional.empty());

        coordinador.ejecutar(entrada, salida);

        assertEquals(404, salida.getStatus());
        verify(transporte, never()).reenviar(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
