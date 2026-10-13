package com.udea.demo.pedidos.application.dto;

public record ResumenTrazabilidadDTO(
        Long pedidoId, EventoTrazabilidadDTO ultimoEvento,
        CheckpointResponseDTO ultimoPuntoControl,
        IncidenciaResponseDTO ultimaIncidencia
) {}
