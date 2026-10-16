package com.udea.demo.rutas.application.service;

import com.udea.demo.pedidos.application.dto.PedidoResponseDTO;
import com.udea.demo.pedidos.domain.model.EstadoPedido;
import com.udea.demo.soporte.DatosDePrueba;
import com.udea.demo.usuarios.domain.model.Conductor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("ValidadorCapacidadRuta - límites de capacidad de entrega, peso y volumen")
class ValidadorCapacidadRutaTest {
    private final ValidadorCapacidadRuta validador = new ValidadorCapacidadRuta();

    private Conductor conductor(Integer maximo, Double peso, Double volumen) {
        return DatosDePrueba.conductor(1L, DatosDePrueba.usuarioConductor(5L, "Conductor Capacidad"),
                peso, volumen, maximo);
    }

    private PedidoResponseDTO pedido(Double pesoKg, Double dimensionCm) {
        return DatosDePrueba.pedido(1L, EstadoPedido.EN_TRANSITO, pesoKg, dimensionCm);
    }

    @Test
    void permiteAsignacionDentroDeLaCapacidad() {
        assertDoesNotThrow(() -> validador.validar(conductor(3, 20.0, 1000.0),
                List.of(pedido(5.0, 2.0)), pedido(6.0, 3.0)));
    }

    @Test
    void rechazaExcesoDeEntregas() {
        assertThrows(IllegalArgumentException.class, () -> validador.validar(
                conductor(2, 100.0, 10000.0), List.of(pedido(1.0, 1.0), pedido(1.0, 1.0)), pedido(1.0, 1.0)));
    }

    @Test
    void rechazaExcesoDePeso() {
        assertThrows(IllegalArgumentException.class, () -> validador.validar(
                conductor(3, 10.0, 10000.0), List.of(pedido(5.0, 1.0)), pedido(6.0, 1.0)));
    }

    @Test
    void rechazaExcesoDeVolumen() {
        assertThrows(IllegalArgumentException.class, () -> validador.validar(
                conductor(3, 100.0, 30.0), List.of(pedido(1.0, 2.0)), pedido(1.0, 3.0)));
    }

    @Test
    void exigeMinimoDeDosEntregasEnLaJornada() {
        assertThrows(IllegalStateException.class, () -> validador.validar(
                conductor(1, 100.0, 10000.0), List.of(), pedido(1.0, 1.0)));
    }
}
