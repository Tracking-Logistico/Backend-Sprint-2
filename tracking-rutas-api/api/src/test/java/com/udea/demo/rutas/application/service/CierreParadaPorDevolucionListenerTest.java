package com.udea.demo.rutas.application.service;

import com.udea.demo.pedidos.domain.event.DevolucionIniciadaEvent;
import com.udea.demo.pedidos.domain.model.EstadoPedido;
import com.udea.demo.pedidos.domain.model.MotivoDevolucion;
import com.udea.demo.pedidos.domain.model.Pedido;
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

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CierreParadaPorDevolucionListener - cierra parada al iniciar devolución")
class CierreParadaPorDevolucionListenerTest {
    @Mock private ParadaRutaRepository paradas;
    @Mock private RutaRepository rutas;
    @InjectMocks private CierreParadaPorDevolucionListener listener;

    private DevolucionIniciadaEvent eventoDe(Pedido pedido) {
        return DevolucionIniciadaEvent.de(pedido, EstadoPedido.EN_REPARTO, MotivoDevolucion.PAQUETE_RECHAZADO);
    }

    @Test
    void cancelaLaParadaPendiente() {
        Pedido pedido = DatosDePrueba.entidadPedido(7L, EstadoPedido.EN_REPARTO);
        Ruta ruta = Ruta.crear(1L, LocalDate.now());
        ParadaRuta parada = ruta.agregarParada(pedido.getId());
        when(paradas.findByPedidoIdAndEstado(pedido.getId(), EstadoParada.PENDIENTE)).thenReturn(Optional.of(parada));

        listener.alIniciarDevolucion(eventoDe(pedido));

        assertThat(parada.getEstado()).isEqualTo(EstadoParada.CANCELADA);
        verify(rutas).save(ruta);
    }

    @Test
    void noHaceNadaSinParadaPendiente() {
        Pedido pedido = DatosDePrueba.entidadPedido(7L, EstadoPedido.EN_REPARTO);
        when(paradas.findByPedidoIdAndEstado(pedido.getId(), EstadoParada.PENDIENTE)).thenReturn(Optional.empty());

        listener.alIniciarDevolucion(eventoDe(pedido));

        verify(rutas, never()).save(any());
    }
}
