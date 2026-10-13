package com.udea.demo.rutas.application.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
public final class SincronizacionEntregasDTO {
    private SincronizacionEntregasDTO() {}
    public record Solicitud(@NotEmpty @Size(max = 100) List<@Valid Item> eventos) {}
    public record Item(@NotNull @Positive Long pedidoId, @NotNull @Valid ResultadoEntregaRequestDTO evento) {}
    public record Resultado(String idEventoCliente, String estado, String motivoRechazo) {}
    public record Respuesta(List<Resultado> resultados) {}
}
