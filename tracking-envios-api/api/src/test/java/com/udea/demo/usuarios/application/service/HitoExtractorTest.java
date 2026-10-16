package com.udea.demo.usuarios.application.service;

import com.udea.demo.compartido.eventos.EventoIntegracion;
import com.udea.demo.pedidos.domain.event.CheckpointRegistradoEvent;
import com.udea.demo.pedidos.domain.event.EntregaConfirmadaEvent;
import com.udea.demo.pedidos.domain.event.EntregaReprogramadaEvent;
import com.udea.demo.pedidos.domain.event.IncidenciaRegistradaEvent;
import com.udea.demo.pedidos.domain.model.EstadoPedido;
import com.udea.demo.pedidos.domain.model.EtapaCheckpoint;
import com.udea.demo.pedidos.domain.model.TipoIncidencia;
import com.udea.demo.usuarios.domain.model.HitoNotificacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HitoExtractor - extracción de hitos notificables para clientes")
class HitoExtractorTest {

    private final HitoExtractor extractor = new HitoExtractor();

    @Test
    @DisplayName("Extrae hito de EntregaConfirmadaEvent")
    void extraer_entregaConfirmada_retornaHito() {
        EntregaConfirmadaEvent evento = new EntregaConfirmadaEvent(
                UUID.randomUUID(), EntregaConfirmadaEvent.TIPO, 1,
                LocalDateTime.now(), 1L, "LT100", 5L, EstadoPedido.ENTREGADO);

        Optional<HitoNotificacion> hito = extractor.extraer(evento);

        assertThat(hito).isPresent();
        assertThat(hito.get().pedidoId()).isEqualTo(1L);
        assertThat(hito.get().numeroTracking()).isEqualTo("LT100");
        assertThat(hito.get().clienteId()).isEqualTo(5L);
        assertThat(hito.get().estadoParaNotificar()).isEqualTo(EstadoPedido.ENTREGADO);
        assertThat(hito.get().tipoEvento()).isEqualTo("ENTREGA_CONFIRMADA");
    }

    @Test
    @DisplayName("Omite CheckpointRegistradoEvent cuando la etapa es RECIBIDO_EN_ORIGEN")
    void extraer_checkpointRecibidoEnOrigen_omite() {
        CheckpointRegistradoEvent evento = new CheckpointRegistradoEvent(
                UUID.randomUUID(), CheckpointRegistradoEvent.TIPO, 1, LocalDateTime.now(),
                2L, "LT200", 6L, 10L, 20L,
                EtapaCheckpoint.RECIBIDO_EN_ORIGEN, EstadoPedido.CREADO, EstadoPedido.RECIBIDO_EN_ORIGEN,
                null, null, false, null, LocalDateTime.now());

        Optional<HitoNotificacion> hito = extractor.extraer(evento);

        assertThat(hito).isEmpty();
    }

    @Test
    @DisplayName("Extrae hito de CheckpointRegistradoEvent para etapas notificables")
    void extraer_checkpointEnTransito_retornaHito() {
        CheckpointRegistradoEvent evento = new CheckpointRegistradoEvent(
                UUID.randomUUID(), CheckpointRegistradoEvent.TIPO, 1, LocalDateTime.now(),
                2L, "LT200", 6L, 10L, 20L,
                EtapaCheckpoint.EN_TRANSITO, EstadoPedido.RECIBIDO_EN_ORIGEN, EstadoPedido.EN_TRANSITO,
                null, null, false, null, LocalDateTime.now());

        Optional<HitoNotificacion> hito = extractor.extraer(evento);

        assertThat(hito).isPresent();
        assertThat(hito.get().estadoParaNotificar()).isEqualTo(EstadoPedido.EN_TRANSITO);
    }

    @Test
    @DisplayName("Extrae hito de IncidenciaRegistradaEvent solo si el estado es ENTREGA_FALLIDA")
    void extraer_incidencia_evaluaEstado() {
        IncidenciaRegistradaEvent eventoFallido = new IncidenciaRegistradaEvent(
                UUID.randomUUID(), IncidenciaRegistradaEvent.TIPO, 1, LocalDateTime.now(),
                3L, "LT300", 7L, 11L, 21L,
                TipoIncidencia.CLIENTE_AUSENTE, TipoIncidencia.CLIENTE_AUSENTE.descripcion(), "Comentario",
                EstadoPedido.EN_REPARTO, EstadoPedido.ENTREGA_FALLIDA, 1, "Cliente ausente");

        Optional<HitoNotificacion> hito = extractor.extraer(eventoFallido);

        assertThat(hito).isPresent();
        assertThat(hito.get().estadoParaNotificar()).isEqualTo(EstadoPedido.ENTREGA_FALLIDA);
        assertThat(hito.get().mensajeCliente()).isEqualTo("Cliente ausente");

        IncidenciaRegistradaEvent eventoOtro = new IncidenciaRegistradaEvent(
                UUID.randomUUID(), IncidenciaRegistradaEvent.TIPO, 1, LocalDateTime.now(),
                3L, "LT300", 7L, 11L, 21L,
                TipoIncidencia.DIRECCION_INCORRECTA, TipoIncidencia.DIRECCION_INCORRECTA.descripcion(), "Comentario",
                EstadoPedido.EN_REPARTO, EstadoPedido.DIRECCION_POR_VERIFICAR, 0, "Dirección errónea");

        assertThat(extractor.extraer(eventoOtro)).isEmpty();
    }

    @Test
    @DisplayName("Extrae hito de EntregaReprogramadaEvent")
    void extraer_entregaReprogramada_retornaHito() {
        EntregaReprogramadaEvent evento = new EntregaReprogramadaEvent(
                UUID.randomUUID(), EntregaReprogramadaEvent.TIPO, 1, LocalDateTime.now(),
                4L, "LT400", 8L, LocalDate.now().plusDays(2), "Reprogramado para el lunes");

        Optional<HitoNotificacion> hito = extractor.extraer(evento);

        assertThat(hito).isPresent();
        assertThat(hito.get().estadoParaNotificar()).isEqualTo(EstadoPedido.ENTREGA_REPROGRAMADA);
    }

    @Test
    @DisplayName("Retorna empty si el evento es desconocido o no implementa los soportados")
    void extraer_desconocido_retornaEmpty() {
        EventoIntegracion evento = new EventoIntegracion() {
            @Override public UUID eventId() { return UUID.randomUUID(); }
            @Override public LocalDateTime ocurridoEn() { return LocalDateTime.now(); }
            @Override public String tipo() { return "OTRO"; }
            @Override public String routingKey() { return "otro"; }
        };

        assertThat(extractor.extraer(evento)).isEmpty();
    }
}
