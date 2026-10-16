package com.udea.demo.pedidos.application.service;

import com.udea.demo.pedidos.application.dto.*;
import com.udea.demo.pedidos.domain.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@DisplayName("PedidoService - delegación a casos de uso con datos reales y tipados")
class PedidoServiceDelegacionTest {
    private final RecepcionPedidoUseCase recepcion = mock(RecepcionPedidoUseCase.class);
    private final ConsultaPedidoUseCase consulta = mock(ConsultaPedidoUseCase.class);
    private final ValidacionPedidoUseCase validacion = mock(ValidacionPedidoUseCase.class);
    private final OperacionLogisticaPedidoUseCase operacion = mock(OperacionLogisticaPedidoUseCase.class);
    private final PedidoService fachada = new PedidoService(recepcion, consulta, validacion, operacion);

    private RecibirPedidoRequestDTO crearRecibirDTO() {
        return new RecibirPedidoRequestDTO(
                "Carrera 7 #71-21, Bogotá", "Bogotá", "110111",
                "Calle 45 #12-30, Bogotá", "Bogotá", "110111",
                "Caja de repuestos electrónicos",
                2.50, 30.0, 20.0, 15.0, TipoServicio.EXPRESS,
                "Carlos Gómez", "+573001234567", "+573109876543");
    }

    private ValidarPedidoRequestDTO crearValidarDTO() {
        return new ValidarPedidoRequestDTO(
                true, Prioridad.ALTA, "Aprobado correctamente",
                "Prioridad confirmada", null, false);
    }

    private PedidoResponseDTO crearPedidoResponse(Long id, EstadoPedido estado) {
        return new PedidoResponseDTO(
                id, "PED-20261015-01", 10L,
                "Carrera 7 #71-21, Bogotá", "Calle 45 #12-30, Bogotá",
                "Caja de repuestos electrónicos",
                2.50, 30.0, 20.0, 15.0, TipoServicio.EXPRESS,
                Prioridad.ALTA, Prioridad.ALTA, estado,
                null, 99L, LocalDateTime.now(), null,
                "TRK-20261015-01", LocalDateTime.now(), false, null);
    }

    @Test
    void delegaLaCreacionSinModificarElContrato() {
        RecibirPedidoRequestDTO solicitud = crearRecibirDTO();
        PedidoResponseDTO respuesta = crearPedidoResponse(4L, EstadoPedido.SOLICITADO);
        when(recepcion.recibir(solicitud)).thenReturn(respuesta);

        assertSame(respuesta, fachada.recibir(solicitud));
        verify(recepcion).recibir(solicitud);
        verifyNoInteractions(consulta, validacion, operacion);
    }

    @Test
    void delegaLaCorreccion() {
        RecibirPedidoRequestDTO solicitud = crearRecibirDTO();
        PedidoResponseDTO respuesta = crearPedidoResponse(4L, EstadoPedido.SOLICITADO);
        when(recepcion.corregir(4L, solicitud)).thenReturn(respuesta);

        assertSame(respuesta, fachada.corregir(4L, solicitud));
        verify(recepcion).corregir(4L, solicitud);
        verifyNoInteractions(consulta, validacion, operacion);
    }

    @Test
    void delegaLaValidacionSinModificarElContrato() {
        ValidarPedidoRequestDTO solicitud = crearValidarDTO();
        PedidoResponseDTO respuesta = crearPedidoResponse(4L, EstadoPedido.SOLICITADO);
        when(validacion.validar(4L, solicitud)).thenReturn(respuesta);

        assertSame(respuesta, fachada.validar(4L, solicitud));
        verify(validacion).validar(4L, solicitud);
        verifyNoInteractions(recepcion, consulta, operacion);
    }

    @Test
    void delegaLaConsultaSinModificarElContrato() {
        PedidoResponseDTO respuesta = crearPedidoResponse(4L, EstadoPedido.SOLICITADO);
        when(consulta.obtener(4L)).thenReturn(respuesta);

        assertSame(respuesta, fachada.obtener(4L));
        verify(consulta).obtener(4L);
        verifyNoInteractions(recepcion, validacion, operacion);
    }

    @Test
    void delegaElTrackingSinModificarElContrato() {
        PedidoResponseDTO respuesta = crearPedidoResponse(4L, EstadoPedido.CREADO);
        when(operacion.activarTracking(4L)).thenReturn(respuesta);

        assertSame(respuesta, fachada.activarTracking(4L));
        verify(operacion).activarTracking(4L);
        verifyNoInteractions(recepcion, consulta, validacion);
    }

    @Test
    void delegaConsultasDeListados() {
        Pageable pageable = PageRequest.of(0, 10);
        PedidoResponseDTO p = crearPedidoResponse(4L, EstadoPedido.SOLICITADO);
        Page<PedidoResponseDTO> pagina = new PageImpl<>(List.of(p));

        when(consulta.listarPendientes(pageable)).thenReturn(pagina);
        assertSame(pagina, fachada.listarPendientes(pageable));

        when(consulta.listarValidados(pageable)).thenReturn(pagina);
        assertSame(pagina, fachada.listarValidados(pageable));

        when(consulta.listarActivables(pageable)).thenReturn(pagina);
        assertSame(pagina, fachada.listarActivables(pageable));

        List<PedidoResponseDTO> despachos = List.of(p);
        when(consulta.listarDespachos()).thenReturn(despachos);
        assertSame(despachos, fachada.listarDespachos());

        when(consulta.listarEnTransito(pageable)).thenReturn(pagina);
        assertSame(pagina, fachada.listarEnTransito(pageable));
    }

    @Test
    void delegaOperacionesLogisticasAdicionales() {
        PedidoResponseDTO p = crearPedidoResponse(4L, EstadoPedido.EN_REPARTO);
        CambiarEstadoLogisticoRequestDTO cambioEstado = new CambiarEstadoLogisticoRequestDTO(EstadoPedido.EN_REPARTO);
        when(operacion.cambiarEstadoLogistico(4L, cambioEstado)).thenReturn(p);
        assertSame(p, fachada.cambiarEstadoLogistico(4L, cambioEstado));

        EtiquetaEnvioResponseDTO etiqueta = new EtiquetaEnvioResponseDTO("PED-01", "TRK-01", "base64content", LocalDateTime.now());
        when(operacion.generarEtiqueta(4L)).thenReturn(etiqueta);
        assertSame(etiqueta, fachada.generarEtiqueta(4L));

        when(consulta.obtenerPorTracking("TRK-01")).thenReturn(p);
        assertSame(p, fachada.obtenerPorTracking("TRK-01"));

        HistorialPedidoResponseDTO h = new HistorialPedidoResponseDTO(1L, 99L, "ESTADO_LOGISTICO", null, "EN_REPARTO", LocalDateTime.now());
        List<HistorialPedidoResponseDTO> historialList = List.of(h);
        when(consulta.historial(4L)).thenReturn(historialList);
        assertSame(historialList, fachada.historial(4L));
    }
}
