package com.udea.demo.pedidos.application.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.udea.demo.pedidos.application.dto.*;
import com.udea.demo.pedidos.domain.exception.PedidoNoEncontradoException;
import com.udea.demo.pedidos.domain.model.*;
import com.udea.demo.pedidos.interfaces.persistence.*;
import com.udea.demo.usuarios.application.service.ActorAuthorizationService;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TrazabilidadService {
    private static final Sort ORDEN = Sort.by(Sort.Order.asc("fecha"), Sort.Order.asc("id"));
    private static final int TAMANO_EXPORTACION = 400;
    private static final int MAX_EVENTOS_EXPORTACION = 50_000;
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final PedidoRepository pedidos;
    private final HistorialPedidoRepository historial;
    private final CheckpointPedidoRepository checkpoints;
    private final IncidenciaPedidoRepository incidencias;
    private final ActorAuthorizationService actores;

    public TrazabilidadService(PedidoRepository pedidos, HistorialPedidoRepository historial,
                               CheckpointPedidoRepository checkpoints, IncidenciaPedidoRepository incidencias,
                               ActorAuthorizationService actores) {
        this.pedidos = pedidos;
        this.historial = historial;
        this.checkpoints = checkpoints;
        this.incidencias = incidencias;
        this.actores = actores;
    }

    @Transactional(readOnly = true)
    public Page<EventoTrazabilidadDTO> consultar(Long pedidoId, String tipo, LocalDateTime desde,
                                                   LocalDateTime hasta, Pageable pageable) {
        verificarAcceso(pedidoId);
        validarFechas(desde, hasta);
        Page<HistorialPedido> eventos = historial.findAll(filtro(pedidoId, tipo, desde, hasta),
                PageRequest.of(pageable.getPageNumber(), Math.min(Math.max(pageable.getPageSize(), 1), 50), ORDEN));
        Map<LocalDateTime, CheckpointPedido> puntos = cargarCheckpoints(pedidoId, eventos.getContent());
        Map<LocalDateTime, IncidenciaPedido> novedades = cargarIncidencias(pedidoId, eventos.getContent());
        return eventos.map(evento -> mapear(evento, puntos, novedades));
    }

    @Transactional(readOnly = true)
    public ResumenTrazabilidadDTO resumen(Long pedidoId) {
        verificarAcceso(pedidoId);
        EventoTrazabilidadDTO ultimo = historial.findFirstByPedidoIdOrderByFechaDescIdDesc(pedidoId)
                .map(evento -> {
                    List<HistorialPedido> unico = List.of(evento);
                    return mapear(evento, cargarCheckpoints(pedidoId, unico), cargarIncidencias(pedidoId, unico));
                }).orElse(null);
        return new ResumenTrazabilidadDTO(pedidoId, ultimo,
                checkpoints.findFirstByPedidoIdOrderByFechaEventoDescIdDesc(pedidoId)
                        .map(CheckpointResponseDTO::de).orElse(null),
                incidencias.findFirstByPedidoIdOrderByFechaDescIdDesc(pedidoId)
                        .map(IncidenciaResponseDTO::de).orElse(null));
    }

    @Transactional
    public EventoTrazabilidadDTO corregir(Long pedidoId, Long eventoId, String motivo) {
        verificarAcceso(pedidoId);
        if (motivo == null || motivo.isBlank() || motivo.length() > 500)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La corrección debe contener entre 1 y 500 caracteres");
        HistorialPedido original = historial.findById(eventoId)
                .filter(e -> e.getPedidoId().equals(pedidoId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado en el envío"));
        HistorialPedido correccion = historial.save(HistorialPedido.correccion(
                pedidoId, actores.actorActual().getId(), original.getId(), motivo.trim()));
        return mapear(correccion, Map.of(), Map.of());
    }

    @Transactional(readOnly = true)
    public byte[] exportar(Long pedidoId, String tipo, LocalDateTime desde, LocalDateTime hasta, String formato) {
        verificarAcceso(pedidoId);
        validarFechas(desde, hasta);
        if (!"csv".equalsIgnoreCase(formato) && !"pdf".equalsIgnoreCase(formato))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "formato debe ser csv o pdf");
        Specification<HistorialPedido> criterios = filtro(pedidoId, tipo, desde, hasta);
        long total = historial.count(criterios);
        if (total > MAX_EVENTOS_EXPORTACION)
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "El historial excede 50000 eventos. Acote las fechas de exportación");
        List<EventoTrazabilidadDTO> eventos = new ArrayList<>((int) total);
        for (int pagina = 0; pagina * TAMANO_EXPORTACION < total; pagina++) {
            List<HistorialPedido> bloque = historial.findAll(criterios,
                    PageRequest.of(pagina, TAMANO_EXPORTACION, ORDEN)).getContent();
            if (bloque.isEmpty()) break;
            Map<LocalDateTime, CheckpointPedido> puntos = cargarCheckpoints(pedidoId, bloque);
            Map<LocalDateTime, IncidenciaPedido> novedades = cargarIncidencias(pedidoId, bloque);
            bloque.forEach(h -> eventos.add(mapear(h, puntos, novedades)));
        }
        return "csv".equalsIgnoreCase(formato) ? csv(eventos) : pdf(pedidoId, eventos);
    }

    private void verificarAcceso(Long pedidoId) {
        actores.operadorActualId();
        if (!pedidos.existsById(pedidoId)) throw new PedidoNoEncontradoException(pedidoId);
    }

    private void validarFechas(LocalDateTime desde, LocalDateTime hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "desde no puede ser posterior a hasta");
    }

    private Specification<HistorialPedido> filtro(Long pedidoId, String tipo,
                                                   LocalDateTime desde, LocalDateTime hasta) {
        String tipoNormalizado = tipo == null || tipo.isBlank() ? null : tipo.trim().toUpperCase(Locale.ROOT);
        return (raiz, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicados = new ArrayList<>();
            predicados.add(cb.equal(raiz.get("pedidoId"), pedidoId));
            if (tipoNormalizado != null) {
                predicados.add(cb.or(cb.equal(raiz.get("tipoEvento"), tipoNormalizado),
                        cb.and(cb.equal(raiz.get("tipoEvento"), "INCIDENCIA"),
                                cb.equal(raiz.get("detalle"), tipoNormalizado))));
            }
            if (desde != null) predicados.add(cb.greaterThanOrEqualTo(raiz.get("fecha"), desde));
            if (hasta != null) predicados.add(cb.lessThanOrEqualTo(raiz.get("fecha"), hasta));
            return cb.and(predicados.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private Map<LocalDateTime, CheckpointPedido> cargarCheckpoints(Long pedidoId, List<HistorialPedido> eventos) {
        Set<LocalDateTime> fechas = eventos.stream()
                .filter(this::esEventoCheckpoint).map(HistorialPedido::getFecha).collect(Collectors.toSet());
        if (fechas.isEmpty()) return Map.of();
        return checkpoints.findByPedidoIdAndFechaEventoIn(pedidoId, fechas).stream()
                .collect(Collectors.toMap(CheckpointPedido::getFechaEvento, c -> c,
                        (a, b) -> a.getId() != null && b.getId() != null && a.getId() > b.getId() ? a : b));
    }

    private Map<LocalDateTime, IncidenciaPedido> cargarIncidencias(Long pedidoId, List<HistorialPedido> eventos) {
        Set<LocalDateTime> fechas = eventos.stream()
                .filter(this::esEventoIncidencia).map(HistorialPedido::getFecha).collect(Collectors.toSet());
        if (fechas.isEmpty()) return Map.of();
        return incidencias.findByPedidoIdAndFechaIn(pedidoId, fechas).stream()
                .collect(Collectors.toMap(IncidenciaPedido::getFecha, i -> i,
                        (a, b) -> a.getId() != null && b.getId() != null && a.getId() > b.getId() ? a : b));
    }

    private boolean esEventoCheckpoint(HistorialPedido h) {
        return "CHECKPOINT".equals(h.getTipoEvento()) || "CHECKPOINT_EN_REVISION".equals(h.getTipoEvento())
                || ("ESTADO_LOGISTICO".equals(h.getTipoEvento()) && "CHECKPOINT".equals(h.getCampoObservado()));
    }

    private boolean esEventoIncidencia(HistorialPedido h) {
        return "INCIDENCIA".equals(h.getTipoEvento())
                || ("ESTADO_LOGISTICO".equals(h.getTipoEvento()) && "INCIDENCIA".equals(h.getCampoObservado()));
    }

    private EventoTrazabilidadDTO mapear(HistorialPedido h, Map<LocalDateTime, CheckpointPedido> puntos,
                                           Map<LocalDateTime, IncidenciaPedido> novedades) {
        CheckpointPedido punto = esEventoCheckpoint(h) ? puntos.get(h.getFecha()) : null;
        IncidenciaPedido novedad = esEventoIncidencia(h) ? novedades.get(h.getFecha()) : null;
        Double lat = punto != null ? punto.getLatitud() : novedad != null ? novedad.getLatitud() : null;
        Double lon = punto != null ? punto.getLongitud() : novedad != null ? novedad.getLongitud() : null;
        String ubicacion = lat == null || lon == null ? "No registrada" : lat + ", " + lon;
        return new EventoTrazabilidadDTO(h.getId(), h.getPedidoId(), h.getUsuarioId(), h.getFecha(),
                h.getTipoEvento(), h.getCampoObservado(), h.getDetalle(), lat, lon, ubicacion,
                punto == null ? null : punto.getEtapa().name(),
                punto == null ? null : punto.getEstadoRegistro().name(), h.getEventoReferenciadoId());
    }

    private byte[] csv(List<EventoTrazabilidadDTO> eventos) {
        StringBuilder contenido = new StringBuilder("\uFEFF");
        contenido.append("id,pedidoId,usuarioId,fecha,tipoEvento,campoObservado,detalle,ubicacion,latitud,longitud,etapa,estadoRegistro,eventoReferenciadoId\r\n");
        for (EventoTrazabilidadDTO e : eventos) {
            Object[] valores = {e.id(), e.pedidoId(), e.usuarioId(), e.fecha(), e.tipoEvento(),
                    e.campoObservado(), e.detalle(), e.ubicacion(), e.latitud(), e.longitud(), e.etapa(),
                    e.estadoRegistro(), e.eventoReferenciadoId()};
            contenido.append(Arrays.stream(valores).map(this::campoCsv).collect(Collectors.joining(","))).append("\r\n");
        }
        return contenido.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String campoCsv(Object valor) {
        String texto = valor == null ? "" : valor.toString();
        texto = texto.replace("\r", " ").replace("\n", " ");
        if (!texto.isBlank() && "=+-@".indexOf(texto.stripLeading().charAt(0)) >= 0) texto = "'" + texto;
        return '"' + texto.replace("\"", "\"\"") + '"';
    }

    private byte[] pdf(Long pedidoId, List<EventoTrazabilidadDTO> eventos) {
        try {
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            Document documento = new Document();
            PdfWriter.getInstance(documento, salida);
            documento.open();
            documento.add(new Paragraph("Trazabilidad cronológica del envío " + pedidoId));
            documento.add(new Paragraph("Total de eventos: " + eventos.size()));
            documento.add(new Paragraph(" "));
            for (EventoTrazabilidadDTO e : eventos) {
                documento.add(new Paragraph("#" + e.id() + " | " + e.fecha().format(FECHA) + " | " + e.tipoEvento()));
                documento.add(new Paragraph("Ubicación: " + e.ubicacion() + " | Etapa: " + sinNull(e.etapa())
                        + " | Estado de registro: " + sinNull(e.estadoRegistro())));
                documento.add(new Paragraph("Detalle: " + sinNull(e.detalle()) + " | Corrección de: "
                        + sinNull(e.eventoReferenciadoId())));
                documento.add(new Paragraph(" "));
            }
            documento.close();
            return salida.toByteArray();
        } catch (DocumentException ex) {
            throw new IllegalStateException("No se pudo generar el PDF de trazabilidad", ex);
        }
    }

    private String sinNull(Object dato) { return dato == null ? "-" : dato.toString(); }
}
