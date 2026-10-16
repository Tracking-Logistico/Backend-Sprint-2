package com.udea.demo.pedidos.application.service;

import com.udea.demo.pedidos.application.dto.CambiarEstadoLogisticoRequestDTO;
import com.udea.demo.pedidos.application.dto.EtiquetaEnvioResponseDTO;
import com.udea.demo.pedidos.application.dto.HistorialPedidoResponseDTO;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("PedidoService - delegación a casos de uso con datos reales y tipados")
class PedidoServiceDelegacionTest {
    private final RecepcionPedidoUseCase recepcion = mock(RecepcionPedidoUseCase.class);
    private final ConsultaPedidoUseCase consulta = mock(ConsultaPedidoUseCase.class);
    private final ValidacionPedidoUseCase validacion = mock(ValidacionPedidoUseCase.class);
    private final OperacionLogisticaPedidoUseCase operacion = mock(OperacionLogisticaPedidoUseCase.class);
    private final PedidoService fachada = new PedidoService(recepcion, consulta, validacion, operacion);

    /** Construye un PedidoResponseDTO representativo con los tipos reales del dominio. */
    private PedidoResponseDTO pedido(Long id, EstadoPedido estado) {
        return new PedidoResponseDTO(
                id,
                "PED-" + id,
                100L,
                "Calle 10 #20-30, Bogotá",
                "Carrera 50 #80-15, Medellín",
                "Caja de ropa",
                5.0,
                30.0,
                20.0,
                10.0,
                TipoServicio.ESTANDAR,
                Prioridad.MEDIA,
                Prioridad.MEDIA,
                estado,
                null,
                null,
                LocalDateTime.of(2026, 10, 1, 9, 0),
                null,
                "TRK-" + id,
                null,
                false,
                null
        );
    }

    /** Construye un RecibirPedidoRequestDTO representativo con los tipos reales. */
    private RecibirPedidoRequestDTO solicitudRecibir() {
        return new RecibirPedidoRequestDTO(
                "Calle 10 #20-30, Bogotá",
                "Bogotá",
                "110111",
                "Carrera 50 #80-15, Medellín",
                "Medellín",
                "050001",
                "Caja de ropa",
                5.0,
                30.0,
                20.0,
                10.0,
                TipoServicio.ESTANDAR,
                "Juan Pérez",
                "+573001234567",
                "+573009876543"
        );
    }

    /** Construye un PedidoClienteResponseDTO a partir de un PedidoResponseDTO. */
    private PedidoClienteResponseDTO pedidoCliente(PedidoResponseDTO p) {
        return new PedidoClienteResponseDTO(
                p.id(),
                p.numeroPedido(),
                p.numeroTracking(),
                p.remitenteNombre(),
                p.destinatarioNombre(),
                p.estado(),
                p.fechaCreacion(),
                p.fechaEstimadaEntrega()
        );
    }

    @Test
    void delegaLaCreacionSinModificarElContrato() {
        RecibirPedidoRequestDTO solicitud = solicitudRecibir();
        PedidoResponseDTO respuesta = pedido(4L, EstadoPedido.SOLICITADO);
        when(recepcion.recibir(solicitud)).thenReturn(respuesta);

        assertSame(respuesta, fachada.recibir(solicitud));
        verify(recepcion).recibir(solicitud);
        verifyNoInteractions(consulta, validacion, operacion);
    }

    @Test
    void delegaLaCorreccion() {
        RecibirPedidoRequestDTO solicitud = solicitudRecibir();
        PedidoResponseDTO respuesta = pedido(4L, EstadoPedido.SOLICITADO);
        when(recepcion.corregir(4L, solicitud)).thenReturn(respuesta);

        assertSame(respuesta, fachada.corregir(4L, solicitud));
        verify(recepcion).corregir(4L, solicitud);
        verifyNoInteractions(consulta, validacion, operacion);
    }

    @Test
    void delegaLaValidacionSinModificarElContrato() {
        PedidoResponseDTO respuesta = pedido(4L, EstadoPedido.SOLICITADO);
        ValidarPedidoRequestDTO solicitud = new ValidarPedidoRequestDTO(
                Boolean.TRUE, respuesta.prioridadSugerida(), "Aprobado correctamente",
                null, null, Boolean.FALSE);
        when(validacion.validar(4L, solicitud)).thenReturn(respuesta);

        assertSame(respuesta, fachada.validar(4L, solicitud));
        verify(validacion).validar(4L, solicitud);
        verifyNoInteractions(recepcion, consulta, operacion);
    }

    @Test
    void delegaLaConsultaSinModificarElContrato() {
        PedidoResponseDTO respuesta = pedido(4L, EstadoPedido.SOLICITADO);
        when(consulta.obtener(4L)).thenReturn(respuesta);

        assertSame(respuesta, fachada.obtener(4L));
        verify(consulta).obtener(4L);
        verifyNoInteractions(recepcion, validacion, operacion);
    }

    @Test
    void delegaElTrackingSinModificarElContrato() {
        PedidoResponseDTO respuesta = pedido(4L, EstadoPedido.CREADO);
        when(operacion.activarTracking(4L)).thenReturn(respuesta);

        assertSame(respuesta, fachada.activarTracking(4L));
        verify(operacion).activarTracking(4L);
        verifyNoInteractions(recepcion, consulta, validacion);
    }

    @Test
    void delegaConsultasDeListados() {
        Pageable pageable = PageRequest.of(0, 10);
        PedidoResponseDTO p = pedido(4L, EstadoPedido.SOLICITADO);
        Page<PedidoResponseDTO> pagina = new PageImpl<>(List.of(p), pageable, 1);
        PedidoClienteResponseDTO mio = pedidoCliente(p);
        Page<PedidoClienteResponseDTO> paginaMios = new PageImpl<>(List.of(mio), pageable, 1);
        LocalDateTime desde = p.fechaCreacion().minusDays(1);
        LocalDateTime hasta = p.fechaCreacion().plusDays(1);

        when(consulta.listarMios(EstadoPedido.SOLICITADO, desde, hasta, pageable)).thenReturn(paginaMios);
        assertSame(paginaMios, fachada.listarMios(EstadoPedido.SOLICITADO, desde, hasta, pageable));

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
        PedidoResponseDTO p = pedido(4L, EstadoPedido.EN_REPARTO);
        CambiarEstadoLogisticoRequestDTO cambioEstado = new CambiarEstadoLogisticoRequestDTO(p.estado());
        when(operacion.cambiarEstadoLogistico(4L, cambioEstado)).thenReturn(p);
        assertSame(p, fachada.cambiarEstadoLogistico(4L, cambioEstado));

        EtiquetaEnvioResponseDTO etiqueta = new EtiquetaEnvioResponseDTO(
                p.numeroPedido(), p.numeroTracking(), p.descripcionPaquete(), p.fechaImpresionEtiqueta());
        when(operacion.generarEtiqueta(4L)).thenReturn(etiqueta);
        assertSame(etiqueta, fachada.generarEtiqueta(4L));

        when(consulta.obtenerPorTracking(p.numeroTracking())).thenReturn(p);
        assertSame(p, fachada.obtenerPorTracking(p.numeroTracking()));

        LocalDateTime ahora = LocalDateTime.of(2026, 10, 1, 9, 0);
        HistorialPedidoResponseDTO entrada = new HistorialPedidoResponseDTO(
                1L, 99L, RegistroHistorialPedido.ESTADO_LOGISTICO,
                null, p.estado().name(), ahora);
        List<HistorialPedidoResponseDTO> historialList = List.of(entrada);
        when(consulta.historial(4L)).thenReturn(historialList);
        assertSame(historialList, fachada.historial(4L));

        when(consulta.historialParaConductor(4L)).thenReturn(historialList);
        assertSame(historialList, fachada.historialParaConductor(4L));

        when(consulta.obtenerParaConductor(4L)).thenReturn(p);
        assertSame(p, fachada.obtenerParaConductor(4L));

        Map<Long, PedidoResponseDTO> porIds = Map.of(p.id(), p);
        when(consulta.obtenerPorIds(List.of(p.id()))).thenReturn(porIds);
        assertSame(porIds, fachada.obtenerPorIds(List.of(p.id())));

        SeguimientoClienteResponseDTO seguimiento = new SeguimientoClienteResponseDTO(
                p.id(), p.numeroPedido(), p.numeroTracking(), p.estado(), p.fechaEstimadaEntrega(),
                List.of(), p.estado().name(), null, p.fechaEntregaReprogramada(), null);
        when(consulta.obtenerSeguimientoCliente(p.numeroTracking())).thenReturn(seguimiento);
        assertSame(seguimiento, fachada.obtenerSeguimientoCliente(p.numeroTracking()));
    }
}
