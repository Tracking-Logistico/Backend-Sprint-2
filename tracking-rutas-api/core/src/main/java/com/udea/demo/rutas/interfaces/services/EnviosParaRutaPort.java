package com.udea.demo.rutas.interfaces.services;

import com.udea.demo.pedidos.application.dto.CambiarEstadoLogisticoRequestDTO;
import com.udea.demo.pedidos.application.dto.PedidoResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EnviosParaRutaPort {
    PedidoResponseDTO obtener(Long pedidoId);
    Page<PedidoResponseDTO> listarEnTransito(Pageable pageable);
    PedidoResponseDTO cambiarEstadoLogistico(Long pedidoId, CambiarEstadoLogisticoRequestDTO cambio);
}
