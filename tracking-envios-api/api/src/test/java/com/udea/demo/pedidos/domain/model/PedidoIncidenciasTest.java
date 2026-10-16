package com.udea.demo.pedidos.domain.model;

import com.udea.demo.pedidos.domain.exception.EnvioFinalizadoException;
import com.udea.demo.pedidos.domain.exception.FechaReprogramacionInvalidaException;
import com.udea.demo.pedidos.domain.exception.PlazoVerificacionDireccionVencidoException;
import com.udea.demo.pedidos.domain.exception.TransicionEstadoInvalidaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("HU-07 Pedido - reglas de incidencias, reprogramación y devoluciones")
class PedidoIncidenciasTest {
    private static final int MAX_INTENTOS = 3;
    private static final int RETENCION = 7;
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 6, 10, 0);
    private static final LocalDate HOY = AHORA.toLocalDate();

    /** Crea un pedido con estado logístico directo (sin pasar por validación ni tracking). */
    private static Pedido pedidoEn(EstadoPedido estado) {
        return Pedido.builder().id(1L).numeroTracking("LT1").estado(estado).build();
    }

    /**
     * Construye un pedido en EN_REPARTO y registra una incidencia CLIENTE_AUSENTE
     * para obtener un pedido en ENTREGA_FALLIDA con {@code fechaEntregaFallida} no nula,
     * tal como lo hace el dominio real.
     */
    private static Pedido pedidoConEntregaFallida() {
        Pedido p = pedidoEn(EstadoPedido.EN_REPARTO);
        p.registrarIncidencia(TipoIncidencia.CLIENTE_AUSENTE, MAX_INTENTOS, AHORA, AHORA.plusDays(3));
        return p;
    }

    private static EstadoPedido registrar(Pedido p, TipoIncidencia tipo) {
        return p.registrarIncidencia(tipo, MAX_INTENTOS, AHORA, AHORA.plusDays(3));
    }

    @Nested
    @DisplayName("Criterio 1: actualización del estado según el tipo")
    class EstadoSegunTipo {

        @Test
        @DisplayName("Cliente ausente → Entrega fallida y cuenta un intento")
        void clienteAusente() {
            Pedido p = pedidoEn(EstadoPedido.EN_REPARTO);

            assertThat(registrar(p, TipoIncidencia.CLIENTE_AUSENTE)).isEqualTo(EstadoPedido.ENTREGA_FALLIDA);
            assertThat(p.getIntentosEntregaFallidos()).isEqualTo(1);
            assertThat(p.getFechaEntregaFallida()).isEqualTo(AHORA);
        }

        @Test
        @DisplayName("Dirección incorrecta → Dirección por verificar con plazo límite")
        void direccionIncorrecta() {
            Pedido p = pedidoEn(EstadoPedido.EN_REPARTO);

            assertThat(registrar(p, TipoIncidencia.DIRECCION_INCORRECTA)).isEqualTo(EstadoPedido.DIRECCION_POR_VERIFICAR);
            assertThat(p.getFechaLimiteVerificacionDireccion()).isEqualTo(AHORA.plusDays(3));
        }

        @Test
        @DisplayName("Paquete rechazado → Devolución al remitente")
        void paqueteRechazado() {
            Pedido p = pedidoEn(EstadoPedido.EN_REPARTO);

            assertThat(registrar(p, TipoIncidencia.PAQUETE_RECHAZADO)).isEqualTo(EstadoPedido.DEVOLUCION_AL_REMITENTE);
        }

        @Test
        @DisplayName("Retraso operativo no altera el estado del pedido")
        void retrasoOperativo() {
            Pedido p = pedidoEn(EstadoPedido.EN_REPARTO);

            assertThat(registrar(p, TipoIncidencia.RETRASO_OPERATIVO)).isEqualTo(EstadoPedido.EN_REPARTO);
        }

        @Test
        @DisplayName("Al alcanzar el máximo de intentos pasa a devolución definitiva")
        void maxIntentosAlcanzado() {
            Pedido p = pedidoEn(EstadoPedido.EN_REPARTO);

            // Intento 1: ENTREGA_FALLIDA
            registrar(p, TipoIncidencia.CLIENTE_AUSENTE);
            // Volver a reparto via reprogramación (flujo real del dominio)
            p.reprogramarEntrega(HOY.plusDays(1), HOY, RETENCION);
            p.cambiarEstadoLogistico(EstadoPedido.EN_REPARTO);

            // Intento 2: ENTREGA_FALLIDA
            registrar(p, TipoIncidencia.CLIENTE_AUSENTE);
            p.reprogramarEntrega(HOY.plusDays(1), HOY, RETENCION);
            p.cambiarEstadoLogistico(EstadoPedido.EN_REPARTO);

            // Intento 3 (máximo): DEVOLUCION_AL_REMITENTE
            assertThat(registrar(p, TipoIncidencia.CLIENTE_AUSENTE)).isEqualTo(EstadoPedido.DEVOLUCION_AL_REMITENTE);
            assertThat(p.getIntentosEntregaFallidos()).isEqualTo(3);
        }

        @Test
        @DisplayName("No se puede registrar incidencia sobre un envío entregado o devuelto")
        void noAplicaAEstadosFinales() {
            Pedido entregado = pedidoEn(EstadoPedido.ENTREGADO);

            assertThatThrownBy(() -> registrar(entregado, TipoIncidencia.CLIENTE_AUSENTE))
                    .isInstanceOf(EnvioFinalizadoException.class);
        }
    }

    @Nested
    @DisplayName("Criterio 3: reprogramación por el cliente")
    class Reprogramacion {

        @Test
        @DisplayName("La ventana de reprogramación cubre desde hoy hasta el límite de retención")
        void ventanaReprogramacion() {
            Pedido p = pedidoConEntregaFallida();
            RangoReprogramacion rango = p.rangoReprogramacion(HOY, RETENCION);

            assertThat(rango.desde()).isEqualTo(HOY);
            assertThat(rango.hasta()).isEqualTo(AHORA.toLocalDate().plusDays(RETENCION));
        }

        @Test
        @DisplayName("Reprogramar en una fecha válida actualiza la entrega y pasa a ENTREGA_REPROGRAMADA")
        void reprogramacionValida() {
            Pedido p = pedidoConEntregaFallida();
            LocalDate fecha = HOY.plusDays(4);

            p.reprogramarEntrega(fecha, HOY, RETENCION);

            assertThat(p.getEstado()).isEqualTo(EstadoPedido.ENTREGA_REPROGRAMADA);
            assertThat(p.getFechaEntregaReprogramada()).isEqualTo(fecha);
        }

        @Test
        @DisplayName("Rechaza reprogramación antes de hoy o después del límite de retención")
        void fechaInvalida() {
            Pedido p = pedidoConEntregaFallida();

            assertThatThrownBy(() -> p.reprogramarEntrega(HOY.minusDays(1), HOY, RETENCION))
                    .isInstanceOf(FechaReprogramacionInvalidaException.class);
            assertThatThrownBy(() -> p.reprogramarEntrega(HOY.plusDays(RETENCION + 1), HOY, RETENCION))
                    .isInstanceOf(FechaReprogramacionInvalidaException.class);
        }

        @ParameterizedTest
        @DisplayName("Solo un pedido con entrega fallida (y fecha registrada) admite reprogramación")
        @EnumSource(value = EstadoPedido.class, names = {"SOLICITADO", "CREADO", "EN_TRANSITO", "EN_REPARTO"})
        void estadoInvalidoParaReprogramar(EstadoPedido estado) {
            Pedido p = pedidoEn(estado);
            assertThatThrownBy(() -> p.reprogramarEntrega(HOY.plusDays(2), HOY, RETENCION))
                    .isInstanceOf(TransicionEstadoInvalidaException.class);
        }
    }

    @Nested
    @DisplayName("Criterio 4: verificación de dirección")
    class VerificacionDireccion {

        /**
         * Construye un pedido en DIRECCION_POR_VERIFICAR a través del flujo real del dominio:
         * primero en EN_REPARTO, luego registra DIRECCION_INCORRECTA.
         */
        private Pedido pedidoConDireccionPorVerificar() {
            Pedido p = pedidoEn(EstadoPedido.EN_REPARTO);
            p.registrarIncidencia(TipoIncidencia.DIRECCION_INCORRECTA, MAX_INTENTOS, AHORA, AHORA.plusDays(3));
            return p;
        }

        @Test
        @DisplayName("Confirmar dirección dentro del plazo vuelve el envío a reparto")
        void confirmaDireccion() {
            Pedido p = pedidoConDireccionPorVerificar();

            p.confirmarDireccion("Calle 100 #15-20", "Bogotá", "110111", AHORA.plusDays(1));

            assertThat(p.getEstado()).isEqualTo(EstadoPedido.EN_REPARTO);
            assertThat(p.getDireccionDestino()).isEqualTo("Calle 100 #15-20");
            assertThat(p.getCiudadDestino()).isEqualTo("Bogotá");
            assertThat(p.getCodigoPostalDestino()).isEqualTo("110111");
        }

        @Test
        @DisplayName("Vencido el plazo límite se escala automáticamente a devolución")
        void escalaPorVencimiento() {
            Pedido p = pedidoConDireccionPorVerificar();

            boolean escalado = p.devolverPorPlazoVencido(AHORA.plusDays(4));

            assertThat(escalado).isTrue();
            assertThat(p.getEstado()).isEqualTo(EstadoPedido.DEVOLUCION_AL_REMITENTE);
        }

        @Test
        @DisplayName("Intentar confirmar dirección pasada la fecha límite es rechazado")
        void confirmarTardeRechaza() {
            Pedido p = pedidoConDireccionPorVerificar();

            assertThatThrownBy(() -> p.confirmarDireccion("Calle 1", "Bogotá", "110111", AHORA.plusDays(4)))
                    .isInstanceOf(PlazoVerificacionDireccionVencidoException.class);
        }
    }
}
