package com.udea.demo.rutas.application.dto;
import java.util.List;
public record RutaPanelConductorDTO(List<Parada> paradas, Parada siguiente) {
    public record Parada(Long paradaId, Long pedidoId, Integer orden, String direccion,
                         String ciudad, String estado, boolean sinUbicacion) {}
}
