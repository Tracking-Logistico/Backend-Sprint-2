package com.udea.demo.pedidos.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CorreccionEventoRequestDTO(
        @NotBlank @Size(max = 500) String motivo
) {}
