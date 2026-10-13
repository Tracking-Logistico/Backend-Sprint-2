package com.udea.demo.rutas.infrastructure.authorization;

import com.udea.demo.pedidos.application.dto.CambiarEstadoLogisticoRequestDTO;
import com.udea.demo.pedidos.application.dto.PedidoResponseDTO;
import com.udea.demo.pedidos.interfaces.services.PedidoServiceI;
import com.udea.demo.rutas.interfaces.services.EnviosParaRutaPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class AdaptadorEnviosParaRuta implements EnviosParaRutaPort {
    private final PedidoServiceI pedidos;

    public AdaptadorEnviosParaRuta(PedidoServiceI pedidos) {
        this.pedidos = pedidos;
    }

    @Override
    public PedidoResponseDTO obtener(Long pedidoId) {
        return pedidos.obtener(pedidoId);
    }

    @Override
    public Page<PedidoResponseDTO> listarEnTransito(Pageable pageable) {
        return pedidos.listarEnTransito(pageable);
    }

    @Override
    public PedidoResponseDTO cambiarEstadoLogistico(Long pedidoId, CambiarEstadoLogisticoRequestDTO cambio) {
        return pedidos.cambiarEstadoLogistico(pedidoId, cambio);
    }
}
