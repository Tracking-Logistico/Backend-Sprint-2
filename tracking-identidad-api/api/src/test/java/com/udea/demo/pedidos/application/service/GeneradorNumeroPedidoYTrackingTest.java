package com.udea.demo.pedidos.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Generadores de códigos de tracking y pedido")
class GeneradorNumeroPedidoYTrackingTest {

    @Test
    @DisplayName("GeneradorNumeroPedido genera formato PED-yyyyMMdd-XXXXXXXX")
    void generarNumeroPedido() {
        GeneradorNumeroPedido generador = new GeneradorNumeroPedido();
        String pedido1 = generador.generar();
        String pedido2 = generador.generar();

        assertThat(pedido1).startsWith("PED-");
        assertThat(pedido1).hasSize(21);
        assertThat(pedido1).isNotEqualTo(pedido2);
    }

    @Test
    @DisplayName("GeneradorNumeroTracking genera 12 caracteres iniciando con LT y sin caracteres confusos")
    void generarNumeroTracking() {
        GeneradorNumeroTracking generador = new GeneradorNumeroTracking();
        String tracking1 = generador.generar();
        String tracking2 = generador.generar();

        assertThat(tracking1).startsWith("LT");
        assertThat(tracking1).hasSize(12);
        assertThat(tracking1).doesNotContain("I", "O", "0", "1");
        assertThat(tracking1).isNotEqualTo(tracking2);
    }
}
