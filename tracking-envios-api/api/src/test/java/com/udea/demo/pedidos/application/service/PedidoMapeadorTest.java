package com.udea.demo.pedidos.application.service;

import com.udea.demo.pedidos.application.dto.PedidoClienteResponseDTO;
import com.udea.demo.pedidos.application.dto.PedidoResponseDTO;
import com.udea.demo.pedidos.domain.model.EstadoPedido;
import com.udea.demo.pedidos.domain.model.Pedido;
import com.udea.demo.pedidos.domain.model.Prioridad;
import com.udea.demo.pedidos.domain.model.TipoServicio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PedidoMapeador - transformación bidireccional y proyección cliente")
class PedidoMapeadorTest {

    private final PedidoMapeador mapeador = new PedidoMapeador();

    @Test
    @DisplayName("map transforma todos los campos a PedidoResponseDTO")
    void map_completo() {
        Pedido p = Pedido.recibir(1L, "Dir O", "Ciudad O", "05001",
                "Dir D", "Ciudad D", "05002", "Paquete grande",
                2.5, 10.0, 20.0, 30.0, TipoServicio.EXPRESS, "PED-123",
                Prioridad.ALTA, "Dest", "300123", "Rem", "rem@test.com", "300456");
        ReflectionTestUtils.setField(p, "id", 100L);
        p.validar(true, Prioridad.ALTA, "Todo bien", 99L);
        p.activarTracking("LT1234567890");

        PedidoResponseDTO dto = mapeador.map(p);

        assertThat(dto.id()).isEqualTo(100L);
        assertThat(dto.numeroPedido()).isEqualTo("PED-123");
        assertThat(dto.numeroTracking()).isEqualTo("LT1234567890");
        assertThat(dto.clienteId()).isEqualTo(1L);
        assertThat(dto.pesoKg()).isEqualTo(2.5);
        assertThat(dto.tipoServicio()).isEqualTo(TipoServicio.EXPRESS);
        assertThat(dto.estado()).isEqualTo(EstadoPedido.CREADO);
        assertThat(dto.observacionesValidacion()).isEqualTo("Todo bien");
    }

    @Test
    @DisplayName("mapCliente calcula fecha estimada si no está presente (1 día express, 3 días estándar)")
    void mapCliente_calculaFechaEstimada() {
        LocalDateTime ahora = LocalDateTime.of(2026, 10, 16, 10, 0);

        Pedido pExpress = Pedido.recibir(1L, "Dir O", "Ciudad O", "05001",
                "Dir D", "Ciudad D", "05002", "Express",
                1.0, 10.0, 10.0, 10.0, TipoServicio.EXPRESS, "PED-E",
                Prioridad.ALTA, "Dest", "300", "Rem", "rem@test.com", "300");
        ReflectionTestUtils.setField(pExpress, "fechaEstimadaEntrega", null);
        ReflectionTestUtils.setField(pExpress, "fechaCreacion", ahora);

        PedidoClienteResponseDTO dtoE = mapeador.mapCliente(pExpress);
        assertThat(dtoE.fechaEstimadaEntrega()).isEqualTo(ahora.plusDays(1));

        Pedido pEstandar = Pedido.recibir(1L, "Dir O", "Ciudad O", "05001",
                "Dir D", "Ciudad D", "05002", "Estandar",
                1.0, 10.0, 10.0, 10.0, TipoServicio.ESTANDAR, "PED-S",
                Prioridad.BAJA, "Dest", "300", "Rem", "rem@test.com", "300");
        ReflectionTestUtils.setField(pEstandar, "fechaEstimadaEntrega", null);
        ReflectionTestUtils.setField(pEstandar, "fechaCreacion", ahora);

        PedidoClienteResponseDTO dtoS = mapeador.mapCliente(pEstandar);
        assertThat(dtoS.fechaEstimadaEntrega()).isEqualTo(ahora.plusDays(3));
    }
}
