package com.udea.demo.pedidos.application.service;

import com.udea.demo.pedidos.application.dto.RecibirPedidoRequestDTO;
import com.udea.demo.pedidos.domain.model.TipoServicio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LimitesServicioService - validaciones de peso, dimensiones, volumen y direcciones")
class LimitesServicioServiceTest {

    private LimitesServicioService service;

    @BeforeEach
    void setUp() {
        service = new LimitesServicioService();
        ReflectionTestUtils.setField(service, "maxWeightKg", 50.0);
        ReflectionTestUtils.setField(service, "maxDimensionCm", 120.0);
        ReflectionTestUtils.setField(service, "maxVolumeCm3", 1000000.0);
        ReflectionTestUtils.setField(service, "expressMaxWeightKg", 20.0);
        ReflectionTestUtils.setField(service, "expressMaxDimensionCm", 80.0);
        ReflectionTestUtils.setField(service, "expressMaxVolumeCm3", 500000.0);
    }

    private RecibirPedidoRequestDTO requestValido(TipoServicio tipo, Double peso, Double l, Double a, Double h) {
        return new RecibirPedidoRequestDTO(
                "Calle 100 #15-20, Bogotá", "Bogotá", "110111",
                "Carrera 7 #72-41, Bogotá", "Bogotá", "110221",
                "Documentos importantes", peso, l, a, h,
                tipo, "Destinatario", "3001234567", "3007654321");
    }

    @Test
    @DisplayName("Pasa validación con datos correctos para servicio ESTANDAR y EXPRESS")
    void validar_exitoso() {
        assertThatCode(() -> service.validar(requestValido(TipoServicio.ESTANDAR, 10.0, 30.0, 30.0, 30.0)))
                .doesNotThrowAnyException();
        assertThatCode(() -> service.validar(requestValido(TipoServicio.EXPRESS, 15.0, 50.0, 50.0, 50.0)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Lanza excepción si tipo de servicio es nulo")
    void validar_tipoServicioNulo_lanzaExcepcion() {
        RecibirPedidoRequestDTO dto = requestValido(null, 10.0, 30.0, 30.0, 30.0);
        assertThatThrownBy(() -> service.validar(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El tipo de servicio es obligatorio");
    }

    @Test
    @DisplayName("Lanza excepción si peso o dimensiones no son números positivos")
    void validar_valoresNoPositivos_lanzaExcepcion() {
        assertThatThrownBy(() -> service.validar(requestValido(TipoServicio.ESTANDAR, -1.0, 10.0, 10.0, 10.0)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("peso");
        assertThatThrownBy(() -> service.validar(requestValido(TipoServicio.ESTANDAR, 1.0, 0.0, 10.0, 10.0)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("largo");
        assertThatThrownBy(() -> service.validar(requestValido(TipoServicio.ESTANDAR, 1.0, 10.0, Double.NaN, 10.0)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("ancho");
        assertThatThrownBy(() -> service.validar(requestValido(TipoServicio.ESTANDAR, 1.0, 10.0, 10.0, null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("alto");
    }

    @Test
    @DisplayName("Lanza excepción si supera peso límite de estándar o express")
    void validar_pesoExcedido_lanzaExcepcion() {
        assertThatThrownBy(() -> service.validar(requestValido(TipoServicio.ESTANDAR, 55.0, 10.0, 10.0, 10.0)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("El peso supera el máximo");
        assertThatThrownBy(() -> service.validar(requestValido(TipoServicio.EXPRESS, 25.0, 10.0, 10.0, 10.0)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("El peso supera el máximo");
    }

    @Test
    @DisplayName("Lanza excepción si supera dimensiones o volumen límite")
    void validar_dimensionesYVolumenExcedidos_lanzaExcepcion() {
        assertThatThrownBy(() -> service.validar(requestValido(TipoServicio.ESTANDAR, 10.0, 130.0, 10.0, 10.0)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Una dimensión supera el máximo");
        assertThatThrownBy(() -> service.validar(requestValido(TipoServicio.EXPRESS, 10.0, 10.0, 85.0, 10.0)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Una dimensión supera el máximo");

        // Volumen estándar límite 1.000.000 cm3: 110 * 100 * 100 = 1.100.000 cm3
        assertThatThrownBy(() -> service.validar(requestValido(TipoServicio.ESTANDAR, 10.0, 110.0, 100.0, 100.0)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("El volumen supera el límite");
    }

    @Test
    @DisplayName("Lanza excepción si direcciones son menores a 8 caracteres o ciudades en blanco")
    void validar_direccionesYCiudades_lanzaExcepcion() {
        RecibirPedidoRequestDTO dtoDirCorta = new RecibirPedidoRequestDTO(
                "Corta", "Bogotá", "110111", "Carrera 7 #72-41, Bogotá", "Bogotá", "110221",
                "Desc", 5.0, 20.0, 20.0, 20.0, TipoServicio.ESTANDAR,
                "Dest", "3001234567", "3007654321");
        assertThatThrownBy(() -> service.validar(dtoDirCorta))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dirección de origen");

        RecibirPedidoRequestDTO dtoDestCorta = new RecibirPedidoRequestDTO(
                "Calle 100 #15-20, Bogotá", "Bogotá", "110111", "Corta", "Bogotá", "110221",
                "Desc", 5.0, 20.0, 20.0, 20.0, TipoServicio.ESTANDAR,
                "Dest", "3001234567", "3007654321");
        assertThatThrownBy(() -> service.validar(dtoDestCorta))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dirección de destino");

        RecibirPedidoRequestDTO dtoCiudadVacia = new RecibirPedidoRequestDTO(
                "Calle 100 #15-20, Bogotá", "", "110111", "Carrera 7 #72-41, Bogotá", "Bogotá", "110221",
                "Desc", 5.0, 20.0, 20.0, 20.0, TipoServicio.ESTANDAR,
                "Dest", "3001234567", "3007654321");
        assertThatThrownBy(() -> service.validar(dtoCiudadVacia))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ciudad de origen y destino");
    }
}
