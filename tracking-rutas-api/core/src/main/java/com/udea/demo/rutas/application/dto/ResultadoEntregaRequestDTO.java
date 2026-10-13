package com.udea.demo.rutas.application.dto;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;

public record ResultadoEntregaRequestDTO(
    @NotNull Resultado resultado,
    String codigoNovedad,
    @Size(max = 500) String motivo,
    Double latitud, Double longitud,
    @NotNull OffsetDateTime fechaEvento,
    @NotBlank @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$") String idEventoCliente
) {
    public enum Resultado { ENTREGADO, ENTREGA_FALLIDA, DEVOLUCION_AL_REMITENTE }
}
