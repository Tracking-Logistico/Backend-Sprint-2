package com.udea.orquestador.infraestructura;

import com.udea.orquestador.aplicacion.CoordinadorSolicitudes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EntradaRestControllerTest {

    @Mock
    private CoordinadorSolicitudes coordinador;
    @InjectMocks
    private EntradaRestController controlador;

    @Test
    void apiDelegaEnElCoordinador() throws Exception {
        MockHttpServletRequest entrada = new MockHttpServletRequest("GET", "/api/v1/auth/login");
        MockHttpServletResponse salida = new MockHttpServletResponse();

        controlador.api(entrada, salida);

        verify(coordinador).ejecutar(entrada, salida);
    }
}
