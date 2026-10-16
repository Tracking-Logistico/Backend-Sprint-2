package com.udea.demo.pedidos.application.service;

import com.udea.demo.pedidos.application.dto.CambiarEstadoLogisticoRequestDTO;
import com.udea.demo.pedidos.application.dto.EtiquetaEnvioResponseDTO;
import com.udea.demo.pedidos.application.dto.HistorialPedidoResponseDTO;
import com.udea.demo.pedidos.application.dto.MovimientoSeguimientoResponseDTO;
import com.udea.demo.pedidos.application.dto.NovedadClienteDTO;
import com.udea.demo.pedidos.application.dto.PedidoClienteResponseDTO;
import com.udea.demo.pedidos.application.dto.PedidoResponseDTO;
import com.udea.demo.pedidos.application.dto.RecibirPedidoRequestDTO;
import com.udea.demo.pedidos.application.dto.SeguimientoClienteResponseDTO;
import com.udea.demo.pedidos.application.dto.ValidarPedidoRequestDTO;
import com.udea.demo.pedidos.domain.model.EstadoPedido;
import com.udea.demo.pedidos.domain.model.Prioridad;
import com.udea.demo.pedidos.domain.model.TipoServicio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("PedidoService - delegación a casos de uso con DTOs reales y tipados")
class PedidoServiceDelegacionTest {
    private final RecepcionPedidoUseCase recepcion = mock(RecepcionPedidoUseCase.class);
    private final ConsultaPedidoUseCase consulta = mock(ConsultaPedidoUseCase.class);
    private final ValidacionPedidoUseCase validacion = mock(ValidacionPedidoUseCase.class);
    private final OperacionLogisticaPedidoUseCase operacion = mock(OperacionLogisticaPedidoUseCase.class);
    private final PedidoService fachada = new PedidoService(recepcion, consulta, validacion, operacion);

    private RecibirPedidoRequestDTO crearRecibirDTO() {
        return new RecibirPedidoRequestDTO(
                "Carrera 43A # 12-34, Medellín", "Medellín", "050021",
                "Calle 10 # 40-20, Medellín", "Medellín", "050016",
                "Documentos corporativos y muestras",
                2.50, 30.0, 20.0, 15.0, TipoServicio.EXPRESS,
                "Carlos Restrepo", "+573001234567", "carlos.restrepo@cliente.com",
                "+573109876543");
    }

    private ValidarPedidoRequestDTO crearValidarDTO() {
        return new ValidarPedidoRequestDTO(
                true, Prioridad.ALTA, "Documentación completa y peso verificado",
                "Cliente con SLA express", null, false);
    }

    private PedidoResponseDTO crearPedidoResponse(Long id, EstadoPedido estado) {
        LocalDateTime fechaCreacion = LocalDateTime.of(2026, 10, 16, 8, 30);
        LocalDateTime fechaValidacion = LocalDateTime.of(2026, 10, 16, 9, 15);
        LocalDateTime fechaTracking = LocalDateTime.of(2026, 10, 16, 9, 20);
        LocalDateTime fechaEstimada = LocalDateTime.of(2026, 10, 17, 18, 0);
        return new PedidoResponseDTO(
                id, "PED-20261016-01", 15L,
                "Carrera 43A # 12-34, Medellín", "Calle 10 # 40-20, Medellín",
                "Documentos corporativos y muestras",
                2.50, 30.0, 20.0, 15.0, TipoServicio.EXPRESS,
                Prioridad.ALTA, Prioridad.ALTA, estado,
                "Documentación completa y peso verificado", 99L,
                fechaCreacion, fechaValidacion,
                "TRK-20261016-0001", fechaTracking, false, null,
                "Carlos Restrepo", "+573001234567", "Cliente con SLA express",
                "Medellín", "Medellín", "050021", "050016",
                "Remitente SAS", "operaciones@remitente.com", "+573109876543",
                fechaEstimada, "Portería principal, citar a recepción",
                LocalDate.of(2026, 10, 18));
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
        PedidoResponseDTO respuesta = crearPedidoResponse(4L, EstadoPedido.VALIDADO);
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
        PedidoResponseDTO pedido = crearPedidoResponse(4L, EstadoPedido.SOLICITADO);
        Page<PedidoResponseDTO> pagina = new PageImpl<>(List.of(pedido), pageable, 1);
        LocalDateTime desde = LocalDateTime.of(2026, 10, 1, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2026, 10, 31, 23, 59);
        PedidoClienteResponseDTO pedidoCliente = new PedidoClienteResponseDTO(
                4L, "PED-20261016-01", "TRK-20261016-0001",
                "Remitente SAS", "Carlos Restrepo", EstadoPedido.SOLICITADO,
                LocalDateTime.of(2026, 10, 16, 8, 30), LocalDateTime.of(2026, 10, 17, 18, 0));
        Page<PedidoClienteResponseDTO> paginaCliente = new PageImpl<>(List.of(pedidoCliente), pageable, 1);

        when(consulta.listarMios(EstadoPedido.SOLICITADO, desde, hasta, pageable)).thenReturn(paginaCliente);
        assertSame(paginaCliente, fachada.listarMios(EstadoPedido.SOLICITADO, desde, hasta, pageable));

        when(consulta.listarPendientes(pageable)).thenReturn(pagina);
        assertSame(pagina, fachada.listarPendientes(pageable));

        when(consulta.listarValidados(pageable)).thenReturn(pagina);
        assertSame(pagina, fachada.listarValidados(pageable));

        when(consulta.listarActivables(pageable)).thenReturn(pagina);
        assertSame(pagina, fachada.listarActivables(pageable));

        List<PedidoResponseDTO> despachos = List.of(pedido);
        when(consulta.listarDespachos()).thenReturn(despachos);
        assertSame(despachos, fachada.listarDespachos());

        when(consulta.listarEnTransito(pageable)).thenReturn(pagina);
        assertSame(pagina, fachada.listarEnTransito(pageable));
    }

    @Test
    void delegaOperacionesLogisticasYConsultasAdicionales() {
        PedidoResponseDTO pedido = crearPedidoResponse(4L, EstadoPedido.EN_REPARTO);
        CambiarEstadoLogisticoRequestDTO cambioEstado = new CambiarEstadoLogisticoRequestDTO(EstadoPedido.EN_REPARTO);
        when(operacion.cambiarEstadoLogistico(4L, cambioEstado)).thenReturn(pedido);
        assertSame(pedido, fachada.cambiarEstadoLogistico(4L, cambioEstado));

        EtiquetaEnvioResponseDTO etiqueta = new EtiquetaEnvioResponseDTO(
                "PED-20261016-01", "TRK-20261016-0001", "JVBERi0xLjQ=", LocalDateTime.of(2026, 10, 16, 9, 25));
        when(operacion.generarEtiqueta(4L)).thenReturn(etiqueta);
        assertSame(etiqueta, fachada.generarEtiqueta(4L));

        when(consulta.obtenerPorTracking("TRK-20261016-0001")).thenReturn(pedido);
        assertSame(pedido, fachada.obtenerPorTracking("TRK-20261016-0001"));

        HistorialPedidoResponseDTO historial = new HistorialPedidoResponseDTO(
                1L, 99L, "ESTADO_LOGISTICO", null, "EN_REPARTO", LocalDateTime.of(2026, 10, 16, 10, 0));
        List<HistorialPedidoResponseDTO> historialList = List.of(historial);
        when(consulta.historial(4L)).thenReturn(historialList);
        assertSame(historialList, fachada.historial(4L));

        when(consulta.historialParaConductor(4L)).thenReturn(historialList);
        assertSame(historialList, fachada.historialParaConductor(4L));

        when(consulta.obtenerParaConductor(4L)).thenReturn(pedido);
        assertSame(pedido, fachada.obtenerParaConductor(4L));

        Map<Long, PedidoResponseDTO> porIds = Map.of(4L, pedido);
        List<Long> ids = List.of(4L);
        when(consulta.obtenerPorIds(ids)).thenReturn(porIds);
        assertSame(porIds, fachada.obtenerPorIds(ids));

        LocalDateTime ahora = LocalDateTime.of(2026, 10, 16, 11, 0);
        SeguimientoClienteResponseDTO seguimiento = new SeguimientoClienteResponseDTO(
                4L, "PED-20261016-01", "TRK-20261016-0001", EstadoPedido.EN_TRANSITO,
                LocalDateTime.of(2026, 10, 17, 18, 0),
                List.of(new MovimientoSeguimientoResponseDTO(
                        EstadoPedido.EN_TRANSITO, ahora, "ESTADO", "El envío salió de origen")),
                "El envío está en tránsito",
                new NovedadClienteDTO("Retraso operativo", "Condiciones de tráfico en el Valle de Aburrá", ahora),
                LocalDate.of(2026, 10, 18), ahora.plusDays(3));
        when(consulta.obtenerSeguimientoCliente("TRK-20261016-0001")).thenReturn(seguimiento);
        assertSame(seguimiento, fachada.obtenerSeguimientoCliente("TRK-20261016-0001"));
    }
}
