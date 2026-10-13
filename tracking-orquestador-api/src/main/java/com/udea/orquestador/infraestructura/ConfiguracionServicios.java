package com.udea.orquestador.infraestructura;

import com.udea.orquestador.dominio.Servicio;
import java.net.URI;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public final class ConfiguracionServicios {
    private final Map<Servicio, URI> destinos = new EnumMap<>(Servicio.class);

    public ConfiguracionServicios(
            @Value("${upstreams.identidad}") URI identidad,
            @Value("${upstreams.envios}") URI envios,
            @Value("${upstreams.rutas}") URI rutas) {
        destinos.put(Servicio.IDENTIDAD, validar(identidad));
        destinos.put(Servicio.ENVIOS, validar(envios));
        destinos.put(Servicio.RUTAS, validar(rutas));
    }

    public URI url(Servicio servicio) {
        return destinos.get(servicio);
    }

    private URI validar(URI uri) {
        if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("Los servicios internos deben usar http o https");
        }
        if (uri.getHost() == null || uri.getUserInfo() != null || uri.getRawPath() != null && !uri.getRawPath().isEmpty() && !"/".equals(uri.getRawPath())
                || uri.getRawQuery() != null || uri.getRawFragment() != null) {
            throw new IllegalArgumentException("La URL interna debe incluir únicamente esquema, host y puerto");
        }
        return uri;
    }
}
