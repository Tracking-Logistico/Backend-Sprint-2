package com.udea.demo.rutas.application.dto;
import java.time.OffsetDateTime;
public record ResultadoEntregaResponseDTO(String idEventoCliente, Long pedidoId,
        ResultadoEntregaRequestDTO.Resultado resultado, String estado, OffsetDateTime fechaEvento, boolean duplicado) {}
