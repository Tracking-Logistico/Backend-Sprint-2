package com.udea.demo.rutas.application.service;

import com.udea.demo.pedidos.application.dto.PedidoResponseDTO;
import com.udea.demo.usuarios.domain.model.Conductor;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ValidadorCapacidadRuta {
    public void validar(Conductor conductor, List<PedidoResponseDTO> actuales, PedidoResponseDTO nuevo) {
        int entregas = actuales.size() + 1;
        double peso = actuales.stream().mapToDouble(PedidoResponseDTO::pesoKg).sum() + nuevo.pesoKg();
        double volumen = actuales.stream().mapToDouble(this::volumen).sum() + volumen(nuevo);
        if (conductor.getMaxEntregasDia() == null || conductor.getMaxEntregasDia() < 2)
            throw new IllegalStateException("La capacidad del conductor debe admitir mínimo 2 entregas");
        if (entregas > conductor.getMaxEntregasDia())
            throw new IllegalArgumentException("El conductor supera el máximo de entregas de la jornada");
        if (peso > conductor.getCapacidadMaxKg())
            throw new IllegalArgumentException("La asignación supera la capacidad máxima de peso del conductor");
        if (volumen > conductor.getCapacidadMaxVolumenCm3())
            throw new IllegalArgumentException("La asignación supera la capacidad máxima de volumen del conductor");
    }

    private double volumen(PedidoResponseDTO pedido) {
        return pedido.largoCm() * pedido.anchoCm() * pedido.altoCm();
    }
}
