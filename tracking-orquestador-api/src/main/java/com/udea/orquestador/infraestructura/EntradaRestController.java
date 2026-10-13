package com.udea.orquestador.infraestructura;

import com.udea.orquestador.aplicacion.CoordinadorSolicitudes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EntradaRestController {
    private final CoordinadorSolicitudes coordinador;

    public EntradaRestController(CoordinadorSolicitudes coordinador) {
        this.coordinador = coordinador;
    }

    @RequestMapping({"/api/v1", "/api/v1/**", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**"})
    public void api(HttpServletRequest entrada, HttpServletResponse salida) throws IOException {
        coordinador.ejecutar(entrada, salida);
    }
}
