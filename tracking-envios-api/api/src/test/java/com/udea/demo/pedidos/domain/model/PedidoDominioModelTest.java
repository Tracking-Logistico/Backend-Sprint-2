package com.udea.demo.pedidos.domain.model;

import com.udea.demo.pedidos.domain.event.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PedidoDominioModelTest - entidades, eventos y value objects del dominio de pedidos")
class PedidoDominioModelTest {

    @Test
    @DisplayName("CheckpointPedido métodos de fábrica y getters")
    void testCheckpointPedido() {
        LocalDateTime ahora = LocalDateTime.now();
        UbicacionReportada ubi = new UbicacionReportada(6.25, -75.59, 10.0, true);

        CheckpointPedido aplicado = CheckpointPedido.aplicado(
                10L, 5L, "evt-1", EtapaCheckpoint.EN_TRANSITO,
                EstadoPedido.RECIBIDO_EN_ORIGEN, EstadoPedido.EN_TRANSITO,
                ubi, OrigenCheckpoint.EN_LINEA, ahora);
        ReflectionTestUtils.setField(aplicado, "id", 1L);

        assertThat(aplicado.getId()).isEqualTo(1L);
        assertThat(aplicado.getPedidoId()).isEqualTo(10L);
        assertThat(aplicado.getUsuarioId()).isEqualTo(5L);
        assertThat(aplicado.getIdEventoCliente()).isEqualTo("evt-1");
        assertThat(aplicado.getEtapa()).isEqualTo(EtapaCheckpoint.EN_TRANSITO);
        assertThat(aplicado.getEstadoAnterior()).isEqualTo(EstadoPedido.RECIBIDO_EN_ORIGEN);
        assertThat(aplicado.getEstadoResultante()).isEqualTo(EstadoPedido.EN_TRANSITO);
        assertThat(aplicado.getEstadoRegistro()).isEqualTo(EstadoRegistroCheckpoint.APLICADO);
        assertThat(aplicado.getOrigen()).isEqualTo(OrigenCheckpoint.EN_LINEA);
        assertThat(aplicado.getLatitud()).isEqualTo(6.25);
        assertThat(aplicado.getLongitud()).isEqualTo(-75.59);
        assertThat(aplicado.getPrecisionMetros()).isEqualTo(10.0);
        assertThat(aplicado.isUbicacionConfiable()).isTrue();
        assertThat(aplicado.cambioEstado()).isTrue();
        assertThat(aplicado.getFechaEvento()).isEqualTo(ahora);
        assertThat(aplicado.getFechaRegistro()).isNotNull();

        CheckpointPedido pendiente = CheckpointPedido.pendienteRevision(
                10L, 5L, "evt-2", EtapaCheckpoint.EN_TRANSITO,
                EstadoPedido.EN_REPARTO, ubi, "Fuera de secuencia", ahora);

        assertThat(pendiente.getEstadoRegistro()).isEqualTo(EstadoRegistroCheckpoint.PENDIENTE_REVISION);
        assertThat(pendiente.getOrigen()).isEqualTo(OrigenCheckpoint.OFFLINE);
        assertThat(pendiente.getMotivoRevision()).isEqualTo("Fuera de secuencia");
        assertThat(pendiente.cambioEstado()).isFalse();
    }

    @Test
    @DisplayName("IncidenciaPedido constructor y getters")
    void testIncidenciaPedido() {
        LocalDateTime ahora = LocalDateTime.now();
        UbicacionReportada ubi = new UbicacionReportada(6.25, -75.59, 10.0, true);

        IncidenciaPedido inc = new IncidenciaPedido(
                20L, 6L, TipoIncidencia.CLIENTE_AUSENTE, "Nadie en casa",
                ubi, EstadoPedido.EN_REPARTO, EstadoPedido.ENTREGA_FALLIDA, 1, ahora);
        ReflectionTestUtils.setField(inc, "id", 3L);

        assertThat(inc.getId()).isEqualTo(3L);
        assertThat(inc.getPedidoId()).isEqualTo(20L);
        assertThat(inc.getUsuarioId()).isEqualTo(6L);
        assertThat(inc.getTipo()).isEqualTo(TipoIncidencia.CLIENTE_AUSENTE);
        assertThat(inc.getComentario()).isEqualTo("Nadie en casa");
        assertThat(inc.getLatitud()).isEqualTo(6.25);
        assertThat(inc.getLongitud()).isEqualTo(-75.59);
        assertThat(inc.getEstadoAnterior()).isEqualTo(EstadoPedido.EN_REPARTO);
        assertThat(inc.getEstadoResultante()).isEqualTo(EstadoPedido.ENTREGA_FALLIDA);
        assertThat(inc.getNumeroIntento()).isEqualTo(1);
        assertThat(inc.getFecha()).isEqualTo(ahora);
    }

    @Test
    @DisplayName("HistorialPedido, RangoReprogramacion y UbicacionReportada")
    void testHistorialYRango() {
        LocalDateTime ahora = LocalDateTime.now();
        HistorialPedido h = HistorialPedido.builder()
                .id(2L).pedidoId(20L).usuarioId(6L).tipoEvento("CHECKPOINT")
                .campoObservado("estado").detalle("Detalle").fecha(ahora).build();

        assertThat(h.getId()).isEqualTo(2L);
        assertThat(h.getPedidoId()).isEqualTo(20L);
        assertThat(h.getUsuarioId()).isEqualTo(6L);
        assertThat(h.getTipoEvento()).isEqualTo("CHECKPOINT");
        assertThat(h.getCampoObservado()).isEqualTo("estado");
        assertThat(h.getDetalle()).isEqualTo("Detalle");
        assertThat(h.getFecha()).isEqualTo(ahora);

        LocalDate desde = LocalDate.now().plusDays(1);
        LocalDate hasta = LocalDate.now().plusDays(5);
        RangoReprogramacion rango = new RangoReprogramacion(desde, hasta);

        assertThat(rango.desde()).isEqualTo(desde);
        assertThat(rango.hasta()).isEqualTo(hasta);
        assertThat(rango.contiene(desde)).isTrue();
        assertThat(rango.contiene(hasta)).isTrue();
        assertThat(rango.contiene(desde.minusDays(1))).isFalse();
    }

    @Test
    @DisplayName("Eventos de dominio de pedidos exponen propiedades correctamente")
    void testDomainEvents() {
        UUID eventId = UUID.randomUUID();
        LocalDateTime ahora = LocalDateTime.now();

        EntregaConfirmadaEvent ece = new EntregaConfirmadaEvent(eventId, "ENTREGA_CONFIRMADA", 1, ahora, 10L, "LT123", 2L, EstadoPedido.ENTREGADO);
        assertThat(ece.eventId()).isEqualTo(eventId);
        assertThat(ece.pedidoId()).isEqualTo(10L);
        assertThat(ece.numeroTracking()).isEqualTo("LT123");
        assertThat(ece.estadoActual()).isEqualTo(EstadoPedido.ENTREGADO);

        DevolucionIniciadaEvent die = new DevolucionIniciadaEvent(eventId, "DEVOLUCION_INICIADA", 1, ahora, 10L, "LT123", 2L, EstadoPedido.EN_REPARTO, MotivoDevolucion.PAQUETE_RECHAZADO, "El destinatario rechazó el paquete", "Devuelto");
        assertThat(die.motivo()).isEqualTo(MotivoDevolucion.PAQUETE_RECHAZADO);

        IncidenciaRegistradaEvent ire = new IncidenciaRegistradaEvent(eventId, "INCIDENCIA", 1, ahora, 10L, "LT123", 2L, 1L, 5L, TipoIncidencia.DIRECCION_INCORRECTA, "Dir erronea", "No existe", EstadoPedido.EN_REPARTO, EstadoPedido.DIRECCION_POR_VERIFICAR, 1, "Verificar");
        assertThat(ire.tipoIncidencia()).isEqualTo(TipoIncidencia.DIRECCION_INCORRECTA);

        CheckpointRegistradoEvent cre = new CheckpointRegistradoEvent(eventId, "CHECKPOINT", 1, ahora, 10L, "LT123", 2L, 1L, 5L, EtapaCheckpoint.EN_TRANSITO, EstadoPedido.RECIBIDO_EN_ORIGEN, EstadoPedido.EN_TRANSITO, 6.2, -75.5, true, OrigenCheckpoint.EN_LINEA, ahora);
        assertThat(cre.etapa()).isEqualTo(EtapaCheckpoint.EN_TRANSITO);
        assertThat(cre.routingKey()).isEqualTo(CheckpointRegistradoEvent.ROUTING_KEY);

        CheckpointPendienteRevisionEvent cpe = new CheckpointPendienteRevisionEvent(eventId, "CHECKPOINT_REVISION", 1, ahora, 10L, "LT123", 1L, 5L, EtapaCheckpoint.EN_TRANSITO, EstadoPedido.EN_REPARTO, "Ubicación sospechosa", ahora);
        assertThat(cpe.motivo()).isEqualTo("Ubicación sospechosa");

        EntregaReprogramadaEvent ere = new EntregaReprogramadaEvent(eventId, "REPROGRAMADA", 1, ahora, 10L, "LT123", 2L, LocalDate.now().plusDays(2), "Cliente reprogramó");
        assertThat(ere.fechaEntregaReprogramada()).isEqualTo(LocalDate.now().plusDays(2));

        VerificacionDireccionSolicitadaEvent vse = new VerificacionDireccionSolicitadaEvent(eventId, "VERIFICACION", 1, ahora, 10L, "LT123", 2L, ahora.plusDays(3), "Confirmar carrera");
        assertThat(vse.fechaLimite()).isEqualTo(ahora.plusDays(3));
    }
}
