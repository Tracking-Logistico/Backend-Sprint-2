package com.udea.demo.pedidos.application.dto;

import java.time.LocalDateTime;

public record EventoTrazabilidadDTO(
        Long id, Long pedidoId, Long usuarioId, LocalDateTime fecha,
        String tipoEvento, String campoObservado, String detalle,
        Double latitud, Double longitud, String ubicacion,
        String etapa, String estadoRegistro, Long eventoReferenciadoId
) {}
