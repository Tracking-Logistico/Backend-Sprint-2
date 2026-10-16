package com.udea.demo.rutas.application.service;

import com.udea.demo.pedidos.application.dto.PedidoResponseDTO;
import com.udea.demo.pedidos.domain.model.EstadoPedido;
import com.udea.demo.pedidos.domain.model.Prioridad;
import com.udea.demo.pedidos.domain.model.TipoServicio;
import com.udea.demo.usuarios.domain.model.Conductor;
import com.udea.demo.usuarios.domain.model.EstadoUsuario;
import com.udea.demo.usuarios.domain.model.Rol;
import com.udea.demo.usuarios.domain.model.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("ValidadorCapacidadRuta - límites de capacidad de entrega, peso y volumen")
class ValidadorCapacidadRutaTest {
    private final ValidadorCapacidadRuta validador = new ValidadorCapacidadRuta();

    private Usuario conductorUsuario() {
        return Usuario.builder()
                .id(7L)
                .nombre("Julián Castaño")
                .email("julian.castano@tracking.com")
                .password("hash-no-usado-en-capacidad")
                .telefono("+573104443322")
                .direccion("Calle 33 # 65-12, Medellín")
                .rol(Rol.CONDUCTOR)
                .estado(EstadoUsuario.ACTIVO)
                .aceptoTerminos(true)
                .activo(true)
                .build();
    }

    private Conductor conductor(Integer maximoEntregas, Double pesoMaxKg, Double volumenMaxCm3) {
        return new Conductor(1L, conductorUsuario(), "LIC-B2-998877", "ACTIVO",
                pesoMaxKg, volumenMaxCm3, maximoEntregas);
    }

    private PedidoResponseDTO pedido(Long id, Double pesoKg, Double largoCm, Double anchoCm, Double altoCm) {
        LocalDateTime fechaCreacion = LocalDateTime.of(2026, 10, 16, 8, 30);
        return new PedidoResponseDTO(
                id, "PED-20261016-" + id, 15L,
                "Carrera 43A # 12-34, Medellín", "Calle 10 # 40-20, Medellín",
                "Documentos corporativos y muestras",
                pesoKg, largoCm, anchoCm, altoCm, TipoServicio.ESTANDAR,
                Prioridad.MEDIA, Prioridad.MEDIA, EstadoPedido.VALIDADO,
                null, 99L, fechaCreacion, fechaCreacion.plusMinutes(20),
                "TRK-20261016-" + id, fechaCreacion.plusMinutes(25), false, null,
                "Carlos Restrepo", "+573001234567", null,
                "Medellín", "Medellín", "050021", "050016",
                "Remitente SAS", "operaciones@remitente.com", "+573109876543",
                LocalDateTime.of(2026, 10, 17, 18, 0), "Portería principal",
                LocalDate.of(2026, 10, 18));
    }

    @Test
    @DisplayName("Permite la asignación si el número de entregas, peso y volumen están dentro de la capacidad")
    void permiteAsignacionDentroDeLaCapacidad() {
        Conductor asignado = conductor(3, 20.0, 1000.0);
        PedidoResponseDTO actual = pedido(101L, 5.0, 2.0, 2.0, 2.0);
        PedidoResponseDTO nuevo = pedido(102L, 6.0, 3.0, 3.0, 3.0);

        assertEquals(Rol.CONDUCTOR, asignado.getUsuario().getRol());
        assertDoesNotThrow(() -> validador.validar(asignado, List.of(actual), nuevo));
    }

    @Test
    @DisplayName("Rechaza la asignación cuando se excede el límite máximo de entregas diarias")
    void rechazaExcesoDeEntregas() {
        assertThrows(IllegalArgumentException.class, () -> validador.validar(
                conductor(2, 100.0, 10000.0),
                List.of(pedido(101L, 1.0, 1.0, 1.0, 1.0), pedido(102L, 1.0, 1.0, 1.0, 1.0)),
                pedido(103L, 1.0, 1.0, 1.0, 1.0)));
    }

    @Test
    @DisplayName("Rechaza la asignación cuando la carga acumulada supera el peso máximo")
    void rechazaExcesoDePeso() {
        assertThrows(IllegalArgumentException.class, () -> validador.validar(
                conductor(3, 10.0, 10000.0),
                List.of(pedido(101L, 5.0, 1.0, 1.0, 1.0)),
                pedido(102L, 6.0, 1.0, 1.0, 1.0)));
    }

    @Test
    @DisplayName("Rechaza la asignación cuando la carga acumulada supera el volumen máximo")
    void rechazaExcesoDeVolumen() {
        assertThrows(IllegalArgumentException.class, () -> validador.validar(
                conductor(3, 100.0, 30.0),
                List.of(pedido(101L, 1.0, 2.0, 2.0, 2.0)),
                pedido(102L, 1.0, 3.0, 3.0, 3.0)));
    }

    @Test
    @DisplayName("Rechaza conductores cuya capacidad diaria no admite el mínimo de 2 entregas")
    void rechazaCapacidadDiariaInsuficiente() {
        assertThrows(IllegalStateException.class, () -> validador.validar(
                conductor(1, 100.0, 10000.0), List.of(), pedido(101L, 1.0, 1.0, 1.0, 1.0)));
    }

    @Test
    @DisplayName("Rechaza conductores sin máximo de entregas diarias configurado")
    void rechazaMaximoDeEntregasNulo() {
        assertThrows(IllegalStateException.class, () -> validador.validar(
                conductor(null, 100.0, 10000.0), List.of(), pedido(101L, 1.0, 1.0, 1.0, 1.0)));
    }
}
