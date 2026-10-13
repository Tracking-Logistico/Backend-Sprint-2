package com.udea.demo.rutas.application.service;

import com.udea.demo.pedidos.application.dto.PedidoResponseDTO;
import com.udea.demo.usuarios.domain.model.Conductor;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ValidadorCapacidadRutaTest {
    private final ValidadorCapacidadRuta validador = new ValidadorCapacidadRuta();

    private Conductor conductor(int maximo, double peso, double volumen) {
        Conductor conductor = mock(Conductor.class);
        when(conductor.getMaxEntregasDia()).thenReturn(maximo);
        when(conductor.getCapacidadMaxKg()).thenReturn(peso);
        when(conductor.getCapacidadMaxVolumenCm3()).thenReturn(volumen);
        return conductor;
    }

    private PedidoResponseDTO pedido(double peso, double dimension) {
        PedidoResponseDTO pedido = mock(PedidoResponseDTO.class);
        when(pedido.pesoKg()).thenReturn(peso);
        when(pedido.largoCm()).thenReturn(dimension);
        when(pedido.anchoCm()).thenReturn(dimension);
        when(pedido.altoCm()).thenReturn(dimension);
        return pedido;
    }

    @Test
    void permiteAsignacionDentroDeLaCapacidad() {
        assertDoesNotThrow(() -> validador.validar(conductor(3, 20, 1000),
                List.of(pedido(5, 2)), pedido(6, 3)));
    }

    @Test
    void rechazaExcesoDeEntregas() {
        assertThrows(IllegalArgumentException.class, () -> validador.validar(
                conductor(2, 100, 10000), List.of(pedido(1, 1), pedido(1, 1)), pedido(1, 1)));
    }

    @Test
    void rechazaExcesoDePeso() {
        assertThrows(IllegalArgumentException.class, () -> validador.validar(
                conductor(3, 10, 10000), List.of(pedido(5, 1)), pedido(6, 1)));
    }

    @Test
    void rechazaExcesoDeVolumen() {
        assertThrows(IllegalArgumentException.class, () -> validador.validar(
                conductor(3, 100, 30), List.of(pedido(1, 2)), pedido(1, 3)));
    }
}
