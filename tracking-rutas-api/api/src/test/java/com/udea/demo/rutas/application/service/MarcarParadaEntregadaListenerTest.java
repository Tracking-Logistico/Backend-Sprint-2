package com.udea.demo.rutas.application.service;

import com.udea.demo.pedidos.domain.event.CheckpointRegistradoEvent;
import com.udea.demo.pedidos.domain.model.CheckpointPedido;
import com.udea.demo.pedidos.domain.model.EstadoPedido;
import com.udea.demo.pedidos.domain.model.EtapaCheckpoint;
import com.udea.demo.pedidos.domain.model.OrigenCheckpoint;
import com.udea.demo.pedidos.domain.model.Pedido;
import com.udea.demo.pedidos.domain.model.UbicacionReportada;
import com.udea.demo.rutas.domain.model.EstadoParada;
import com.udea.demo.rutas.domain.model.ParadaRuta;
import com.udea.demo.rutas.domain.model.Ruta;
import com.udea.demo.rutas.interfaces.persistence.ParadaRutaRepository;
import com.udea.demo.rutas.interfaces.persistence.RutaRepository;
import com.udea.demo.soporte.DatosDePrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MarcarParadaEntregadaListener - cierra parada al checkpoint ENTREGADO")
class MarcarParadaEntregadaListenerTest {
    @Mock private ParadaRutaRepository paradas;
    @Mock private RutaRepository rutas;
    @InjectMocks private MarcarParadaEntregadaListener listener;

    private CheckpointRegistradoEvent evento(Pedido pedido, EstadoPedido estadoActual) {
        CheckpointPedido checkpoint = CheckpointPedido.aplicado(
                pedido.getId(), 5L, "evt-" + pedido.getId(), EtapaCheckpoint.ENTREGADO,
                EstadoPedido.EN_REPARTO, estadoActual,
                new UbicacionReportada(6.2442, -75.5812, 12.0, true),
                OrigenCheckpoint.EN_LINEA, LocalDateTime.now());
        ReflectionTestUtils.setField(checkpoint, "id", 80L);
        return CheckpointRegistradoEvent.de(pedido, checkpoint);
    }

    @Test
    void marcaLaParadaCuandoElEstadoEsEntregado() {
        Pedido pedido = DatosDePrueba.entidadPedido(7L, EstadoPedido.EN_REPARTO);
        Ruta ruta = Ruta.crear(1L, LocalDate.now());
        ParadaRuta parada = ruta.agregarParada(pedido.getId());
        when(paradas.findByPedidoIdAndEstado(pedido.getId(), EstadoParada.PENDIENTE)).thenReturn(Optional.of(parada));

        listener.alRegistrarCheckpoint(evento(pedido, EstadoPedido.ENTREGADO));

        assertThat(parada.getEstado()).isEqualTo(EstadoParada.ENTREGADO);
        verify(rutas).save(ruta);
    }

    @Test
    void ignoraCheckpointsQueNoCierranLaEntrega() {
        Pedido pedido = DatosDePrueba.entidadPedido(7L, EstadoPedido.EN_REPARTO);

        listener.alRegistrarCheckpoint(evento(pedido, EstadoPedido.EN_REPARTO));

        verify(paradas, never()).findByPedidoIdAndEstado(any(), any());
        verify(rutas, never()).save(any());
    }
}
