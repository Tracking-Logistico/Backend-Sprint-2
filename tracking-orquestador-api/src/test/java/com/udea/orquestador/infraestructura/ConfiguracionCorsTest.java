package com.udea.orquestador.infraestructura;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.CorsFilter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfiguracionCorsTest {

    private final ConfiguracionCors configuracion = new ConfiguracionCors();

    @Test
    void preflightPermiteElOrigenConfigurado() throws Exception {
        CorsFilter filtro = configuracion.filtroCors("http://localhost:5173", false);
        MockHttpServletRequest entrada = preflight("http://localhost:5173");
        MockHttpServletResponse salida = new MockHttpServletResponse();

        filtro.doFilter(entrada, salida, cadenaVacia());

        assertEquals("http://localhost:5173", salida.getHeader("Access-Control-Allow-Origin"));
        assertEquals("true", salida.getHeader("Access-Control-Allow-Credentials"));
        assertTrue(salida.getHeader("Access-Control-Allow-Methods").contains("POST"));
        assertTrue(salida.getHeader("Access-Control-Expose-Headers").contains("Content-Disposition"));
    }

    @Test
    void preflightRechazaOrigenFueraDeLaLista() throws Exception {
        CorsFilter filtro = configuracion.filtroCors("http://localhost:5173", false);
        MockHttpServletRequest entrada = preflight("https://otro-origen.example");
        MockHttpServletResponse salida = new MockHttpServletResponse();

        filtro.doFilter(entrada, salida, cadenaVacia());

        assertNull(salida.getHeader("Access-Control-Allow-Origin"));
    }

    @Test
    void demoAbiertoAceptaCualquierOrigenSinCredenciales() throws Exception {
        CorsFilter filtro = configuracion.filtroCors("http://localhost:5173", true);
        MockHttpServletRequest entrada = preflight("https://logistrack-frontend-demo.onrender.com");
        MockHttpServletResponse salida = new MockHttpServletResponse();

        filtro.doFilter(entrada, salida, cadenaVacia());

        assertEquals("https://logistrack-frontend-demo.onrender.com",
                salida.getHeader("Access-Control-Allow-Origin"));
        assertNull(salida.getHeader("Access-Control-Allow-Credentials"));
    }

    @Test
    void ignoraOrigenesEnBlancoEnLaLista() throws Exception {
        CorsFilter filtro = configuracion.filtroCors("http://localhost:5173,  ,", false);
        MockHttpServletRequest entrada = preflight("http://localhost:5173");
        MockHttpServletResponse salida = new MockHttpServletResponse();

        filtro.doFilter(entrada, salida, cadenaVacia());

        assertEquals("http://localhost:5173", salida.getHeader("Access-Control-Allow-Origin"));
    }

    @Test
    void admiteVariosOrigenesSeparadosPorComa() throws Exception {
        CorsFilter filtro = configuracion.filtroCors(
                "http://localhost:5173, https://logistrack-frontend-demo.onrender.com", false);
        MockHttpServletRequest entrada = preflight("https://logistrack-frontend-demo.onrender.com");
        MockHttpServletResponse salida = new MockHttpServletResponse();

        filtro.doFilter(entrada, salida, cadenaVacia());

        assertEquals("https://logistrack-frontend-demo.onrender.com",
                salida.getHeader("Access-Control-Allow-Origin"));
    }

    private static MockHttpServletRequest preflight(String origen) {
        MockHttpServletRequest entrada = new MockHttpServletRequest("OPTIONS", "/api/v1/auth/login");
        entrada.addHeader("Origin", origen);
        entrada.addHeader("Access-Control-Request-Method", "POST");
        return entrada;
    }

    private static FilterChain cadenaVacia() {
        return (request, response) -> { };
    }
}
