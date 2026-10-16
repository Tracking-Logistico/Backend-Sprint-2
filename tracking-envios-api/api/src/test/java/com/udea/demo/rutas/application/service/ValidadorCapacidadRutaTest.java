package com.udea.demo.rutas.application.service;

import com.udea.demo.pedidos.application.dto.PedidoResponseDTO;
import com.udea.demo.pedidos.domain.model.EstadoPedido;
import com.udea.demo.pedidos.domain.model.Prioridad;
import com.udea.demo.pedidos.domain.model.TipoServicio;
import com.udea.demo.usuarios.domain.model.Conductor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("ValidadorCapacidadRuta - límites de capacidad de entrega, peso y volumen")
class ValidadorCapacidadRutaTest {
    private final ValidadorCapacidadRuta validador = new ValidadorCapacidadRuta();

    private Conductor conductor(int maximo, double peso, double volumen) {
        return new Conductor(1L, null, "LIC-COL-2026", "ACTIVO", peso, volumen, maximo);
    }

    private PedidoResponseDTO pedido(double peso, double dimension) {
        return new PedidoResponseDTO(
                1L, "PED-20261015-01", 10L, "Carrera 7 #71-21, Bogotá", "Calle 45 #12-30, Bogotá",
                "Caja de suministros", peso, dimension, dimension, dimension,
                TipoServicio.ESTANDAR,
                Prioridad.MEDIA,
                Prioridad.MEDIA,
                EstadoPedido.SOLICITADO,
                null, null, LocalDateTime.now(), null,
                "TRK-20261015-01", LocalDateTime.now(), false, null);
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
