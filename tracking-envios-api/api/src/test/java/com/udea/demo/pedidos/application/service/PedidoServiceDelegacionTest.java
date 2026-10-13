package com.udea.demo.pedidos.application.service;

import com.udea.demo.pedidos.application.dto.PedidoResponseDTO;
import com.udea.demo.pedidos.application.dto.RecibirPedidoRequestDTO;
import com.udea.demo.pedidos.application.dto.ValidarPedidoRequestDTO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

class PedidoServiceDelegacionTest {
    private final RecepcionPedidoUseCase recepcion = mock(RecepcionPedidoUseCase.class);
    private final ConsultaPedidoUseCase consulta = mock(ConsultaPedidoUseCase.class);
    private final ValidacionPedidoUseCase validacion = mock(ValidacionPedidoUseCase.class);
    private final OperacionLogisticaPedidoUseCase operacion = mock(OperacionLogisticaPedidoUseCase.class);
    private final PedidoService fachada = new PedidoService(recepcion, consulta, validacion, operacion);

    @Test
    void delegaLaCreacionSinModificarElContrato() {
        RecibirPedidoRequestDTO solicitud = mock(RecibirPedidoRequestDTO.class);
        PedidoResponseDTO respuesta = mock(PedidoResponseDTO.class);
        when(recepcion.recibir(solicitud)).thenReturn(respuesta);
        assertSame(respuesta, fachada.recibir(solicitud));
        verify(recepcion).recibir(solicitud);
        verifyNoInteractions(consulta, validacion, operacion);
    }

    @Test
    void delegaLaValidacionSinModificarElContrato() {
        ValidarPedidoRequestDTO solicitud = mock(ValidarPedidoRequestDTO.class);
        PedidoResponseDTO respuesta = mock(PedidoResponseDTO.class);
        when(validacion.validar(4L, solicitud)).thenReturn(respuesta);
        assertSame(respuesta, fachada.validar(4L, solicitud));
        verify(validacion).validar(4L, solicitud);
        verifyNoInteractions(recepcion, consulta, operacion);
    }

    @Test
    void delegaLaConsultaSinModificarElContrato() {
        PedidoResponseDTO respuesta = mock(PedidoResponseDTO.class);
        when(consulta.obtener(4L)).thenReturn(respuesta);
        assertSame(respuesta, fachada.obtener(4L));
        verify(consulta).obtener(4L);
        verifyNoInteractions(recepcion, validacion, operacion);
    }

    @Test
    void delegaElTrackingSinModificarElContrato() {
        PedidoResponseDTO respuesta = mock(PedidoResponseDTO.class);
        when(operacion.activarTracking(4L)).thenReturn(respuesta);
        assertSame(respuesta, fachada.activarTracking(4L));
        verify(operacion).activarTracking(4L);
        verifyNoInteractions(recepcion, consulta, validacion);
    }
}
