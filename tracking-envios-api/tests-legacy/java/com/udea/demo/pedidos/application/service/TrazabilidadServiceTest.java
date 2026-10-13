package com.udea.demo.pedidos.application.service;

import com.udea.demo.pedidos.domain.model.*;
import com.udea.demo.pedidos.interfaces.persistence.*;
import com.udea.demo.usuarios.application.service.ActorAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrazabilidadServiceTest {
    @Mock PedidoRepository pedidos;
    @Mock HistorialPedidoRepository historial;
    @Mock CheckpointPedidoRepository checkpoints;
    @Mock IncidenciaPedidoRepository incidencias;
    @Mock ActorAuthorizationService actores;

    private TrazabilidadService servicio;
    private static final Long ID = 7L;
    private final LocalDateTime fecha = LocalDateTime.of(2026, 10, 10, 8, 30);

    @BeforeEach
    void configurar() {
        servicio = new TrazabilidadService(pedidos, historial, checkpoints, incidencias, actores);
        lenient().when(pedidos.existsById(ID)).thenReturn(true);
    }

    private HistorialPedido evento(Long id, String tipo, String campo, String detalle) {
        return HistorialPedido.builder().id(id).pedidoId(ID).usuarioId(12L).tipoEvento(tipo)
                .campoObservado(campo).detalle(detalle).fecha(fecha).build();
    }

    @Test
    void consultaCronologicaConUbicacionYLimiteDePagina() {
        var h = evento(8L, "CHECKPOINT", null, "En tránsito");
        when(historial.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(inv -> {
                    Pageable pg = inv.getArgument(1);
                    assertThat(pg.getPageSize()).isEqualTo(50);
                    assertThat(pg.getSort().getOrderFor("fecha").getDirection()).isEqualTo(Sort.Direction.ASC);
                    return new PageImpl<>(List.of(h));
                });
        CheckpointPedido checkpoint = CheckpointPedido.aplicado(ID, 12L, "uuid-test", EtapaCheckpoint.values()[0],
                EstadoPedido.EN_TRANSITO, EstadoPedido.EN_TRANSITO,
                new UbicacionReportada(6.2442, -75.5812, 10.0, true), OrigenCheckpoint.EN_LINEA, fecha);
        when(checkpoints.findByPedidoIdAndFechaEventoIn(eq(ID), anyCollection()))
                .thenReturn(List.of(checkpoint));

        var resultado = servicio.consultar(ID, "CHECKPOINT", null, null, PageRequest.of(0, 999));

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).ubicacion()).isEqualTo("6.2442, -75.5812");
        assertThat(resultado.getContent().get(0).tipoEvento()).isEqualTo("CHECKPOINT");
        verify(actores).operadorActualId();
    }

    @Test
    void ubicaUltimoPuntoDeControlYUltimoEvento() {
        var h = evento(8L, "INCIDENCIA", null, "RETRASO_OPERATIVO");
        when(historial.findFirstByPedidoIdOrderByFechaDescIdDesc(ID)).thenReturn(Optional.of(h));
        when(incidencias.findByPedidoIdAndFechaIn(eq(ID), anyCollection())).thenReturn(List.of());
        CheckpointPedido checkpoint = CheckpointPedido.aplicado(ID, 12L, "uuid", EtapaCheckpoint.values()[0],
                EstadoPedido.EN_TRANSITO, EstadoPedido.EN_TRANSITO,
                new UbicacionReportada(6.2, -75.5, 9.0, true), OrigenCheckpoint.EN_LINEA, fecha.minusHours(1));
        when(checkpoints.findFirstByPedidoIdOrderByFechaEventoDescIdDesc(ID)).thenReturn(Optional.of(checkpoint));

        var resumen = servicio.resumen(ID);

        assertThat(resumen.ultimoEvento().tipoEvento()).isEqualTo("INCIDENCIA");
        assertThat(resumen.ultimoPuntoControl().latitud()).isEqualTo(6.2);
    }

    @Test
    void correccionMantieneOriginalYReferenciaElEvento() {
        var original = evento(8L, "INCIDENCIA", null, "PAQUETE_DANADO");
        when(historial.findById(8L)).thenReturn(Optional.of(original));
        when(actores.actorActual()).thenReturn(com.udea.demo.usuarios.domain.model.Usuario.builder().id(12L).build());
        when(historial.save(any(HistorialPedido.class))).thenAnswer(inv -> inv.getArgument(0));

        var correccion = servicio.corregir(ID, 8L, "La ubicación reportada no es correcta");

        assertThat(correccion.tipoEvento()).isEqualTo("CORRECCION_EVENTO");
        assertThat(correccion.eventoReferenciadoId()).isEqualTo(8L);
        assertThat(original.getDetalle()).isEqualTo("PAQUETE_DANADO");
        verify(historial, never()).delete(any(HistorialPedido.class));
    }

    @Test
    void bloqueaCorreccionEnEventoDeOtroPedido() {
        var deOtro = HistorialPedido.builder().id(88L).pedidoId(999L).fecha(fecha).build();
        when(historial.findById(88L)).thenReturn(Optional.of(deOtro));
        assertThatThrownBy(() -> servicio.corregir(ID, 88L, "Corregido"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
        verify(historial, never()).save(any());
    }

    @Test
    void exportaCsvEscapandoFormulasYCreaPdf() {
        var evento = evento(1L, "INCIDENCIA", null, "=HYPERLINK(\"peligro\")");
        when(historial.count(any(Specification.class))).thenReturn(1L);
        when(historial.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(evento)));
        byte[] csv = servicio.exportar(ID, null, null, null, "csv");
        String texto = new String(csv, StandardCharsets.UTF_8);
        assertThat(texto).contains("'=").contains("HYPERLINK").contains("eventoReferenciadoId");

        byte[] pdf = servicio.exportar(ID, null, null, null, "pdf");
        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }

    @Test
    void rechazaRangoInvalidoYFormatoNoPermitido() {
        assertThatThrownBy(() -> servicio.consultar(ID, null, fecha.plusDays(1), fecha, PageRequest.of(0, 10)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
        assertThatThrownBy(() -> servicio.exportar(ID, null, null, null, "html"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
    }

    @Test
    void noConsultaHistorialSiElEnvioNoExiste() {
        when(pedidos.existsById(999L)).thenReturn(false);
        assertThatThrownBy(() -> servicio.consultar(999L, null, null, null, PageRequest.of(0, 10)))
                .isInstanceOf(com.udea.demo.pedidos.domain.exception.PedidoNoEncontradoException.class);
        verify(historial, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void noPermiteEditarNiEliminarUnaEntradaPersistida() throws Exception {
        var registro = evento(1L, "INCIDENCIA", null, "Retraso");
        var actualizar = HistorialPedido.class.getDeclaredMethod("impedirMutacion");
        actualizar.setAccessible(true);
        assertThatThrownBy(() -> actualizar.invoke(registro))
                .hasRootCauseInstanceOf(IllegalStateException.class);
    }
}
