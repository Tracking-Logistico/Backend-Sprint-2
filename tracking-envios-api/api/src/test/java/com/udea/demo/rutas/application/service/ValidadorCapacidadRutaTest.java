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

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("ValidadorCapacidadRuta - límites de capacidad de entrega, peso y volumen")
class ValidadorCapacidadRutaTest {
    private final ValidadorCapacidadRuta validador = new ValidadorCapacidadRuta();

    /** Crea un Usuario con rol CONDUCTOR activo. */
    private Usuario usuarioConductor(Long id, String nombre) {
        return Usuario.builder()
                .id(id)
                .nombre(nombre)
                .email("conductor" + id + "@test.com")
                .password("hash-seguro")
                .rol(Rol.CONDUCTOR)
                .estado(EstadoUsuario.ACTIVO)
                .aceptoTerminos(Boolean.TRUE)
                .activo(Boolean.TRUE)
                .build();
    }

    /** Crea un Conductor con capacidades específicas. */
    private Conductor conductor(Integer maximo, Double pesoMaxKg, Double volumenMaxCm3) {
        return Conductor.builder()
                .id(1L)
                .usuario(usuarioConductor(5L, "Conductor Capacidad"))
                .licencia("LIC-001")
                .estado("ACTIVO")
                .capacidadMaxKg(pesoMaxKg)
                .capacidadMaxVolumenCm3(volumenMaxCm3)
                .maxEntregasDia(maximo)
                .build();
    }

    /** Crea un PedidoResponseDTO con peso y dimensiones específicos. */
    private PedidoResponseDTO pedido(Double pesoKg, Double dimensionCm) {
        return new PedidoResponseDTO(
                1L,
                "PED-001",
                100L,
                "Calle 10 #20-30, Bogotá",
                "Carrera 50 #80-15, Medellín",
                "Caja de prueba",
                pesoKg,
                dimensionCm,
                dimensionCm,
                dimensionCm,
                TipoServicio.ESTANDAR,
                Prioridad.MEDIA,
                Prioridad.MEDIA,
                EstadoPedido.EN_TRANSITO,
                null,
                null,
                LocalDateTime.of(2026, 10, 1, 9, 0),
                null,
                "TRK-001",
                null,
                false,
                null
        );
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
