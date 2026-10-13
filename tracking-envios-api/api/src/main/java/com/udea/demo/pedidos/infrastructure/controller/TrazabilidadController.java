package com.udea.demo.pedidos.infrastructure.controller;

import com.udea.demo.pedidos.application.dto.*;
import com.udea.demo.pedidos.application.service.TrazabilidadService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/pedidos/{id}/trazabilidad")
public class TrazabilidadController {
    private final TrazabilidadService servicio;

    public TrazabilidadController(TrazabilidadService servicio) { this.servicio = servicio; }

    @GetMapping
    public Page<EventoTrazabilidadDTO> consultar(@PathVariable Long id,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1) throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.BAD_REQUEST, "page debe ser >= 0 y size >= 1");
        return servicio.consultar(id, tipo, desde, hasta, PageRequest.of(page, Math.min(size, 50)));
    }

    @GetMapping("/resumen")
    public ResumenTrazabilidadDTO resumen(@PathVariable Long id) {
        return servicio.resumen(id);
    }

    @PostMapping("/eventos/{eventoId}/correcciones")
    public ResponseEntity<EventoTrazabilidadDTO> corregir(@PathVariable Long id, @PathVariable Long eventoId,
                                     @Valid @RequestBody CorreccionEventoRequestDTO solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.corregir(id, eventoId, solicitud.motivo()));
    }

    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar(@PathVariable Long id,
            @RequestParam(defaultValue = "csv") String formato,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        boolean pdf = "pdf".equalsIgnoreCase(formato);
        byte[] contenido = servicio.exportar(id, tipo, desde, hasta, formato);
        return ResponseEntity.ok()
                .contentType(pdf ? MediaType.APPLICATION_PDF : new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("trazabilidad-pedido-" + id + (pdf ? ".pdf" : ".csv")).build().toString())
                .body(contenido);
    }
}
