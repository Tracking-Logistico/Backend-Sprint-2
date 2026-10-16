package com.udea.demo.rutas.application.service;

import com.udea.demo.pedidos.application.dto.PedidoResponseDTO;
import com.udea.demo.pedidos.domain.model.EstadoPedido;
import com.udea.demo.pedidos.domain.model.Prioridad;
import com.udea.demo.pedidos.domain.model.TipoServicio;
import com.udea.demo.rutas.application.dto.AsignacionMasivaRequestDTO;
import com.udea.demo.rutas.application.dto.ConductorDisponibleDTO;
import com.udea.demo.rutas.application.dto.ReasignarEnvioRequestDTO;
import com.udea.demo.rutas.domain.model.EstadoParada;
import com.udea.demo.rutas.domain.model.ParadaRuta;
import com.udea.demo.rutas.domain.model.Ruta;
import com.udea.demo.rutas.interfaces.persistence.ParadaRutaRepository;
import com.udea.demo.rutas.interfaces.persistence.RutaRepository;
import com.udea.demo.rutas.interfaces.services.EnviosParaRutaPort;
import com.udea.demo.usuarios.domain.model.Conductor;
import com.udea.demo.usuarios.domain.model.Rol;
import com.udea.demo.usuarios.domain.model.Usuario;
import com.udea.demo.usuarios.interfaces.persistence.ConductorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RutaServiceComplementoTest - casos de validación y conductores disponibles")
class RutaServiceComplementoTest {

    @Mock private RutaRepository rutas;
    @Mock private ParadaRutaRepository paradas;
    @Mock private ConductorRepository conductores;
    @Mock private EnviosParaRutaPort pedidos;
    @Mock private GestorRutaActiva gestor;
    @Mock private ValidadorCapacidadRuta capacidad;
    @Mock private RutaMapeador mapeador;
    @Mock private RegistroAsignacionService registro;

    @InjectMocks
    private RutaService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "zonaHoraria", "America/Bogota");
    }

    private PedidoResponseDTO crearPedido(Long id, EstadoPedido estado) {
        return new PedidoResponseDTO(
                id, "PED-" + id, 1L, "Dir O", "Dir D", "Paquete",
                2.0, 10.0, 10.0, 10.0, TipoServicio.ESTANDAR, Prioridad.MEDIA, Prioridad.MEDIA,
                estado, null, null, LocalDateTime.now(), null, "LT-" + id,
                null, false, null, "Dest", "300", null, "Med", "Med",
                "05001", "05002", "Rem", "rem@test.com", "300", null, null, null);
    }

    @Test
    @DisplayName("asignarVarios lanza IllegalArgumentException con lista nula, vacía o con duplicados")
    void asignarVarios_validacionesLista_lanzaExcepcion() {
        assertThatThrownBy(() -> service.asignarVarios(new AsignacionMasivaRequestDTO(1L, null)))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> service.asignarVarios(new AsignacionMasivaRequestDTO(1L, List.of())))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> service.asignarVarios(new AsignacionMasivaRequestDTO(1L, List.of(1L, 1L))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("asignarVarios lanza IllegalStateException si el pedido no está en estado recibible")
    void asignarVarios_estadoInvalido_lanzaExcepcion() {
        Conductor conductor = Conductor.builder()
                .id(10L).estado("ACTIVO").usuario(Usuario.builder().id(1L).rol(Rol.CONDUCTOR).activo(true).build()).build();
        when(conductores.findByUsuarioIdForUpdate(1L)).thenReturn(Optional.of(conductor));
        when(gestor.obtenerOCrear(10L)).thenReturn(Ruta.builder().id(5L).build());
        when(paradas.findByPedidoIdAndEstado(100L, EstadoParada.PENDIENTE)).thenReturn(Optional.empty());

        PedidoResponseDTO pedidoEntregado = crearPedido(100L, EstadoPedido.ENTREGADO);
        when(pedidos.obtener(100L)).thenReturn(pedidoEntregado);

        AsignacionMasivaRequestDTO dto = new AsignacionMasivaRequestDTO(1L, List.of(100L));
        assertThatThrownBy(() -> service.asignarVarios(dto))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("reasignarEnvio lanza IllegalArgumentException si nuevo conductor es igual al actual")
    void reasignarEnvio_mismoConductor_lanzaExcepcion() {
        Ruta rutaActual = Ruta.builder().id(1L).conductorId(10L).build();
        ParadaRuta parada = ParadaRuta.builder().id(2L).ruta(rutaActual).pedidoId(50L).estado(EstadoParada.PENDIENTE).build();
        when(paradas.findByPedidoIdAndEstado(50L, EstadoParada.PENDIENTE)).thenReturn(Optional.of(parada));

        Conductor condActual = Conductor.builder().id(10L).usuario(Usuario.builder().id(5L).build()).build();
        when(conductores.findById(10L)).thenReturn(Optional.of(condActual));

        ReasignarEnvioRequestDTO req = new ReasignarEnvioRequestDTO(50L, 5L, "Mismo");
        assertThatThrownBy(() -> service.reasignarEnvio(req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("reasignarEnvio lanza IllegalStateException si pedido no está en reparto")
    void reasignarEnvio_pedidoNoEnReparto_lanzaExcepcion() {
        Ruta rutaActual = Ruta.builder().id(1L).conductorId(10L).build();
        ParadaRuta parada = ParadaRuta.builder().id(2L).ruta(rutaActual).pedidoId(50L).estado(EstadoParada.PENDIENTE).build();
        when(paradas.findByPedidoIdAndEstado(50L, EstadoParada.PENDIENTE)).thenReturn(Optional.of(parada));

        Conductor condActual = Conductor.builder().id(10L).usuario(Usuario.builder().id(5L).build()).build();
        when(conductores.findById(10L)).thenReturn(Optional.of(condActual));

        Conductor condNuevo = Conductor.builder().id(20L).estado("ACTIVO")
                .usuario(Usuario.builder().id(6L).rol(Rol.CONDUCTOR).activo(true).build()).build();
        when(conductores.findByUsuarioIdForUpdate(6L)).thenReturn(Optional.of(condNuevo));
        when(gestor.obtenerOCrear(20L)).thenReturn(Ruta.builder().id(2L).build());

        PedidoResponseDTO pedido = crearPedido(50L, EstadoPedido.CREADO);
        when(pedidos.obtener(50L)).thenReturn(pedido);

        ReasignarEnvioRequestDTO req = new ReasignarEnvioRequestDTO(50L, 6L, "Cambio");
        assertThatThrownBy(() -> service.reasignarEnvio(req))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("listarConductoresDisponibles mapea métricas de carga del conductor")
    void listarConductoresDisponibles_mapeaMetricas() {
        Conductor c = Conductor.builder().id(10L).estado("ACTIVO")
                .capacidadMaxKg(100.0).capacidadMaxVolumenCm3(50000.0).maxEntregasDia(20)
                .usuario(Usuario.builder().id(5L).nombre("Conductor A").rol(Rol.CONDUCTOR).activo(true).build()).build();
        when(conductores.findAll()).thenReturn(List.of(c));

        Ruta r = Ruta.builder().id(1L).conductorId(10L).build();
        ParadaRuta p = ParadaRuta.builder().id(1L).ruta(r).pedidoId(100L).estado(EstadoParada.PENDIENTE).build();
        r.getParadas().add(p);
        when(rutas.findByConductorIdAndFecha(any(), any())).thenReturn(Optional.of(r));

        PedidoResponseDTO ped = crearPedido(100L, EstadoPedido.EN_REPARTO);
        when(pedidos.obtener(100L)).thenReturn(ped);

        List<ConductorDisponibleDTO> res = service.listarConductoresDisponibles();

        assertThat(res).hasSize(1);
        assertThat(res.get(0).nombre()).isEqualTo("Conductor A");
        assertThat(res.get(0).entregasAsignadas()).isEqualTo(1);
        assertThat(res.get(0).pesoAsignadoKg()).isEqualTo(2.0);
    }
}
