package com.udea.orquestador.aplicacion;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;

public interface TransporteHttp {
    void reenviar(URI base, HttpServletRequest entrada, HttpServletResponse salida) throws IOException;
}
