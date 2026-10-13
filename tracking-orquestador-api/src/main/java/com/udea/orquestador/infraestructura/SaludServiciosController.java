package com.udea.orquestador.infraestructura;

import com.udea.orquestador.dominio.Servicio;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SaludServiciosController {
    private final ConfiguracionServicios destinos;

    public SaludServiciosController(ConfiguracionServicios destinos) {
        this.destinos = destinos;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> estado() {
        Map<String, String> estado = new LinkedHashMap<>();
        for (Servicio servicio : Servicio.values()) {
            URI url = destinos.url(servicio);
            int puerto = url.getPort() == -1 ? ("https".equalsIgnoreCase(url.getScheme()) ? 443 : 80) : url.getPort();
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(url.getHost(), puerto), 1000);
                estado.put(servicio.name().toLowerCase(), "UP");
            } catch (Exception ex) {
                estado.put(servicio.name().toLowerCase(), "DOWN");
            }
        }
        boolean disponible = estado.values().stream().allMatch("UP"::equals);
        return ResponseEntity.status(disponible ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("estado", disponible ? "UP" : "DEGRADED", "servicios", estado));
    }
}
