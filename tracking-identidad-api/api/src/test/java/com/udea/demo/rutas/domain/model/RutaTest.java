package com.udea.demo.rutas.domain.model;

import com.udea.demo.rutas.domain.exception.EnvioNoAsignadoException;
import com.udea.demo.rutas.domain.exception.OrdenInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Ruta - dominio de paradas y orden")
class RutaTest {
    private static final Long CONDUCTOR_ID = 1L;
    private static final Long PEDIDO_ID_A = 10L;
    private static final Long PEDIDO_ID_B = 20L;
    private static final Long PEDIDO_ID_C = 30L;

    private Ruta rutaVacia() {
        return Ruta.crear(CONDUCTOR_ID, LocalDate.now());
    }

    @Test
    void crearInicializaSinParadas() {
        LocalDate hoy = LocalDate.now();
        Ruta ruta = Ruta.crear(CONDUCTOR_ID, hoy);
        assertThat(ruta.getConductorId()).isEqualTo(CONDUCTOR_ID);
        assertThat(ruta.getFecha()).isEqualTo(hoy);
        assertThat(ruta.getParadas()).isEmpty();
        assertThat(ruta.getFechaCreacion()).isNotNull();
    }

    @Nested
    class AgregarParada {
        @Test
        void primeraParadaQuedaPendienteConOrdenUno() {
            ParadaRuta parada = rutaVacia().agregarParada(PEDIDO_ID_A);
            assertThat(parada.getPedidoId()).isEqualTo(PEDIDO_ID_A);
            assertThat(parada.getOrden()).isEqualTo(1);
            assertThat(parada.getEstado()).isEqualTo(EstadoParada.PENDIENTE);
        }

        @Test
        void incrementaElOrden() {
            Ruta ruta = rutaVacia();
            ruta.agregarParada(PEDIDO_ID_A);
            assertThat(ruta.agregarParada(PEDIDO_ID_B).getOrden()).isEqualTo(2);
        }
    }

    @Nested
    class Reordenar {
        @Test
        void aplicaElOrdenIndicada() {
            Ruta ruta = rutaVacia();
            ruta.agregarParada(PEDIDO_ID_A);
            ruta.agregarParada(PEDIDO_ID_B);
            ruta.agregarParada(PEDIDO_ID_C);
            ruta.reordenar(List.of(PEDIDO_ID_C, PEDIDO_ID_B, PEDIDO_ID_A));
            assertThat(ruta.getParadas().stream()
                    .filter(p -> p.getPedidoId().equals(PEDIDO_ID_C)).findFirst().orElseThrow().getOrden())
                    .isEqualTo(1);
        }

        @Test
        void rechazaUnPedidoAjeno() {
            Ruta ruta = rutaVacia();
            ruta.agregarParada(PEDIDO_ID_A);
            ruta.agregarParada(PEDIDO_ID_B);
            assertThatThrownBy(() -> ruta.reordenar(List.of(PEDIDO_ID_A, PEDIDO_ID_C)))
                    .isInstanceOf(OrdenInvalidoException.class);
        }
    }

    @Nested
    class CancelarParada {
        @Test
        void dejaLaParadaCancelada() {
            Ruta ruta = rutaVacia();
            ruta.agregarParada(PEDIDO_ID_A);
            ruta.cancelarParada(PEDIDO_ID_A);
            assertThat(ruta.getParadas().get(0).getEstado()).isEqualTo(EstadoParada.CANCELADA);
        }

        @Test
        void fallaSiNoEstaPendiente() {
            Ruta ruta = rutaVacia();
            ruta.agregarParada(PEDIDO_ID_A);
            assertThatThrownBy(() -> ruta.cancelarParada(PEDIDO_ID_B))
                    .isInstanceOf(EnvioNoAsignadoException.class);
        }
    }

    @Test
    void conservaOrdenManualAlAgregarOtraParada() {
        Ruta ruta = Ruta.crear(5L, LocalDate.now());
        ruta.agregarParada(PEDIDO_ID_A);
        ruta.agregarParada(PEDIDO_ID_B);
        ruta.reordenar(List.of(PEDIDO_ID_B, PEDIDO_ID_A));
        ruta.agregarParada(PEDIDO_ID_C);
        assertThat(ruta.isOrdenManual()).isTrue();
        assertThat(ruta.getParadas().stream()
                .filter(p -> p.getEstado() == EstadoParada.PENDIENTE)
                .sorted(java.util.Comparator.comparing(ParadaRuta::getOrden))
                .map(ParadaRuta::getPedidoId).toList())
                .containsExactly(PEDIDO_ID_B, PEDIDO_ID_A, PEDIDO_ID_C);
    }
}
