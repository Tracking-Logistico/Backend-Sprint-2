package com.udea.orquestador.aplicacion;

import com.udea.orquestador.dominio.Servicio;
import com.udea.orquestador.infraestructura.ConfiguracionServicios;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Service;

@Service
public class CoordinadorSolicitudes {
    private final Enrutador enrutador;
    private final TransporteHttp transporte;
    private final ConfiguracionServicios servicios;

    public CoordinadorSolicitudes(Enrutador enrutador, TransporteHttp transporte, ConfiguracionServicios servicios) {
        this.enrutador = enrutador;
        this.transporte = transporte;
        this.servicios = servicios;
    }

    public void ejecutar(HttpServletRequest entrada, HttpServletResponse salida) throws IOException {
        Servicio servicio = enrutador.resolver(entrada.getRequestURI()).orElse(null);
        if (servicio == null) {
            salida.sendError(404);
            return;
        }
        transporte.reenviar(servicios.url(servicio), entrada, salida);
    }
}
