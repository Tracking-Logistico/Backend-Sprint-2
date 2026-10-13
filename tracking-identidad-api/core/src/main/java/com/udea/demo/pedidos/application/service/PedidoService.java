package com.udea.demo.pedidos.application.service;

import com.udea.demo.pedidos.application.dto.*;
import com.udea.demo.pedidos.domain.model.*;
import com.udea.demo.pedidos.interfaces.services.PedidoServiceI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PedidoService implements PedidoServiceI {
    private final RecepcionPedidoUseCase recepcion;
    private final ConsultaPedidoUseCase consulta;
    private final ValidacionPedidoUseCase validacion;
    private final OperacionLogisticaPedidoUseCase operacion;

    public PedidoService(RecepcionPedidoUseCase recepcion, ConsultaPedidoUseCase consulta,
            ValidacionPedidoUseCase validacion, OperacionLogisticaPedidoUseCase operacion) {
        this.recepcion = recepcion;
        this.consulta = consulta;
        this.validacion = validacion;
        this.operacion = operacion;
    }

    @Override public PedidoResponseDTO recibir(RecibirPedidoRequestDTO dto) {
        return recepcion.recibir(dto);
    }

    @Override
    public PedidoResponseDTO corregir(Long id, RecibirPedidoRequestDTO dto) {
        return recepcion.corregir(id, dto);
    }

    @Override public Page<PedidoClienteResponseDTO> listarMios(EstadoPedido estado, LocalDateTime fechaDesde,
                                                      LocalDateTime fechaHasta, Pageable pageable) {
        return consulta.listarMios(estado, fechaDesde, fechaHasta, pageable);
    }

    @Override public Page<PedidoResponseDTO> listarPendientes(Pageable pageable) {
        return consulta.listarPendientes(pageable);
    }

    @Override public Page<PedidoResponseDTO> listarValidados(Pageable pageable) {
        return consulta.listarValidados(pageable);
    }

    @Override public Page<PedidoResponseDTO> listarActivables(Pageable pageable) {
        return consulta.listarActivables(pageable);
    }

    @Override public List<PedidoResponseDTO> listarDespachos() {
        return consulta.listarDespachos();
    }

    @Override public Page<PedidoResponseDTO> listarEnTransito(Pageable pageable) {
        return consulta.listarEnTransito(pageable);
    }

    @Override public PedidoResponseDTO obtener(Long id) {
        return consulta.obtener(id);
    }

    @Override public PedidoResponseDTO obtenerPorTracking(String numeroTracking) {
        return consulta.obtenerPorTracking(numeroTracking);
    }

    @Override public SeguimientoClienteResponseDTO obtenerSeguimientoCliente(String numeroTracking) {
        return consulta.obtenerSeguimientoCliente(numeroTracking);
    }

    @Override public List<HistorialPedidoResponseDTO> historial(Long id) {
        return consulta.historial(id);
    }

    @Override public java.util.Map<Long, PedidoResponseDTO> obtenerPorIds(java.util.Collection<Long> ids) {
        return consulta.obtenerPorIds(ids);
    }

    @Override public PedidoResponseDTO obtenerParaConductor(Long pedidoId) {
        return consulta.obtenerParaConductor(pedidoId);
    }

    @Override public List<HistorialPedidoResponseDTO> historialParaConductor(Long id) {
        return consulta.historialParaConductor(id);
    }

    @Override public PedidoResponseDTO validar(Long id, ValidarPedidoRequestDTO dto) {
        return validacion.validar(id, dto);
    }

    @Override public PedidoResponseDTO activarTracking(Long id) {
        return operacion.activarTracking(id);
    }

    @Override public PedidoResponseDTO cambiarEstadoLogistico(Long id, CambiarEstadoLogisticoRequestDTO dto) {
        return operacion.cambiarEstadoLogistico(id, dto);
    }

    @Override public EtiquetaEnvioResponseDTO generarEtiqueta(Long id) {
        return operacion.generarEtiqueta(id);
    }
}
