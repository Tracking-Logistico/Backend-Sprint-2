package com.udea.demo.rutas.application.service;

import com.udea.demo.pedidos.application.dto.PedidoResponseDTO;
import com.udea.demo.rutas.application.dto.*;
import com.udea.demo.rutas.domain.model.*;
import com.udea.demo.rutas.interfaces.services.EnviosParaRutaPort;
import com.udea.demo.usuarios.interfaces.persistence.ConductorRepository;
import java.util.List;
import java.util.Comparator;
import org.springframework.stereotype.Component;

@Component
public class RutaMapeador {
    private final EnviosParaRutaPort pedidos;
    private final ConductorRepository conductores;

    public RutaMapeador(EnviosParaRutaPort pedidos, ConductorRepository conductores) {
        this.pedidos = pedidos;
        this.conductores = conductores;
    }

    public RutaResponseDTO map(Ruta ruta) {
        List<ParadaResponseDTO> items = ruta.getParadas().stream()
                .filter(p -> p.getEstado() == EstadoParada.PENDIENTE)
                .sorted(Comparator.comparing(ParadaRuta::getOrden))
                .map(p -> {
                    PedidoResponseDTO pedido = pedidos.obtener(p.getPedidoId());
                    return new ParadaResponseDTO(p.getId(), p.getPedidoId(), p.getOrden(), p.getEstado().name(),
                        p.getFechaAsignacion(), pedido.numeroPedido(), pedido.numeroTracking(), pedido.direccionDestino(),
                        pedido.ciudadDestino(), pedido.destinatarioNombre(), pedido.destinatarioTelefono(),
                        (pedido.prioridadConfirmada() != null ? pedido.prioridadConfirmada() : pedido.prioridadSugerida()).name(),
                        pedido.pesoKg());
                }).toList();
        Long usuarioId = conductores.findById(ruta.getConductorId()).map(c -> c.getUsuario().getId()).orElse(ruta.getConductorId());
        return new RutaResponseDTO(ruta.getId(), usuarioId, ruta.getFecha(), items);
    }
}
