package com.udea.demo.rutas.domain.model;

import com.udea.demo.rutas.application.dto.ResultadoEntregaRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EventoResultadoEntrega - entidad de registro de eventos de entrega")
class EventoResultadoEntregaTest {

    @Test
    @DisplayName("Crea y expone propiedades del evento")
    void testEventoResultadoEntrega() {
        OffsetDateTime ahora = OffsetDateTime.now();
        ResultadoEntregaRequestDTO req = new ResultadoEntregaRequestDTO(
                ResultadoEntregaRequestDTO.Resultado.ENTREGADO,
                "NINGUNA", "Entregado a destinatario", 6.2, -75.5, ahora, "11111111-1111-1111-1111-111111111111");

        EventoResultadoEntrega evento = new EventoResultadoEntrega("11111111-1111-1111-1111-111111111111", 1L, 2L, req);

        assertThat(evento.getIdEventoCliente()).isEqualTo("11111111-1111-1111-1111-111111111111");
        assertThat(evento.getPedidoId()).isEqualTo(1L);
        assertThat(evento.getUsuarioId()).isEqualTo(2L);
        assertThat(evento.getResultado()).isEqualTo(ResultadoEntregaRequestDTO.Resultado.ENTREGADO);
        assertThat(evento.getCodigoNovedad()).isEqualTo("NINGUNA");
        assertThat(evento.getMotivo()).isEqualTo("Entregado a destinatario");
        assertThat(evento.getFechaEvento()).isEqualTo(ahora);
    }
}
