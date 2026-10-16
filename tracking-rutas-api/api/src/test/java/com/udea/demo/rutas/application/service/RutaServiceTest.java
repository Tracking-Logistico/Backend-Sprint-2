package com.udea.demo.rutas.application.service;

import com.udea.demo.pedidos.application.dto.PedidoResponseDTO;
import com.udea.demo.pedidos.domain.model.EstadoPedido;
import com.udea.demo.rutas.application.dto.AsignacionMasivaRequestDTO;
import com.udea.demo.rutas.application.dto.AsignarEnvioRequestDTO;
import com.udea.demo.rutas.application.dto.ReasignarEnvioRequestDTO;
import com.udea.demo.rutas.application.dto.ReordenarRutaRequestDTO;
import com.udea.demo.rutas.application.dto.RutaResponseDTO;
import com.udea.demo.rutas.domain.exception.EnvioNoAsignadoException;
import com.udea.demo.rutas.domain.exception.EnvioYaAsignadoException;
import com.udea.demo.rutas.domain.exception.RutaNoEncontradaException;
import com.udea.demo.rutas.domain.model.EstadoParada;
import com.udea.demo.rutas.domain.model.ParadaRuta;
import com.udea.demo.rutas.domain.model.Ruta;
import com.udea.demo.rutas.interfaces.persistence.ParadaRutaRepository;
import com.udea.demo.rutas.interfaces.persistence.RutaRepository;
import com.udea.demo.rutas.interfaces.services.EnviosParaRutaPort;
import com.udea.demo.soporte.DatosDePrueba;
import com.udea.demo.usuarios.domain.model.Conductor;
import com.udea.demo.usuarios.interfaces.persistence.ConductorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RutaService - asignación y organización con EnviosParaRutaPort")
class RutaServiceTest {
    private static final String ZONA = "America/Bogota";
    private static final Pageable PAGINA = PageRequest.of(0, 20);
    private static final Long USUARIO_CONDUCTOR = 5L;
    private static final Long CONDUCTOR_INTERNO = 1L;
    private static final Long USUARIO_NUEVO = 6L;
    private static final Long CONDUCTOR_NUEVO = 2L;
    private static final Long PEDIDO_ID = 10L;
    private static final Long RUTA_ID = 100L;

    @Mock private RutaRepository rutas;
    @Mock private ParadaRutaRepository paradas;
    @Mock private GestorRutaActiva gestor;
    @Mock private EnviosParaRutaPort envios;
    @Mock private ConductorRepository conductores;
    @Mock private RegistroAsignacionService registro;

    private RutaService rutaService;

    @BeforeEach
    void armarServicio() {
        RutaMapeador mapeador = new RutaMapeador(envios, conductores);
        rutaService = new RutaService(rutas, paradas, gestor, envios, conductores, registro,
                mapeador, new ValidadorCapacidadRuta());
        ReflectionTestUtils.setField(rutaService, "zonaHoraria", ZONA);
        lenient().when(envios.obtener(any(Long.class)))
                .thenAnswer(inv -> DatosDePrueba.pedido(inv.getArgument(0), EstadoPedido.EN_TRANSITO));
        lenient().when(envios.cambiarEstadoLogistico(any(), any()))
                .thenAnswer(inv -> DatosDePrueba.pedido(inv.getArgument(0), EstadoPedido.EN_REPARTO));
    }

    private LocalDate hoy() {
        return LocalDate.now(ZoneId.of(ZONA));
    }

    private Ruta rutaConParadas(Long conductorId, Long... pedidoIds) {
        Ruta ruta = Ruta.crear(conductorId, hoy());
        for (Long pedidoId : pedidoIds) {
            ruta.agregarParada(pedidoId);
        }
        return ruta;
    }

    @Nested
    @DisplayName("listarEnviosPendientesDeAsignacion")
    class ListarPendientes {
        @Test
        void omiteLosEnviosQueYaTienenParadaPendiente() {
            PedidoResponseDTO libre = DatosDePrueba.pedido(PEDIDO_ID, EstadoPedido.EN_TRANSITO);
            PedidoResponseDTO asignado = DatosDePrueba.pedido(20L, EstadoPedido.EN_TRANSITO);
            when(paradas.findPedidoIdsByEstado(EstadoParada.PENDIENTE)).thenReturn(List.of(20L));
            when(envios.listarEnTransito(PAGINA)).thenReturn(new PageImpl<>(List.of(libre, asignado), PAGINA, 2));

            List<PedidoResponseDTO> resultado = rutaService.listarEnviosPendientesDeAsignacion(PAGINA).getContent();

            assertThat(resultado).containsExactly(libre);
        }

        @Test
        void listaVaciaSiNoHayEnviosEnTransito() {
            when(paradas.findPedidoIdsByEstado(EstadoParada.PENDIENTE)).thenReturn(List.of());
            when(envios.listarEnTransito(PAGINA)).thenReturn(new PageImpl<>(List.of(), PAGINA, 0));

            assertThat(rutaService.listarEnviosPendientesDeAsignacion(PAGINA).getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("asignarEnvio")
    class Asignar {
        @Test
        void asociaElPedidoALaRutaDelConductor() {
            AsignarEnvioRequestDTO dto = new AsignarEnvioRequestDTO(PEDIDO_ID, USUARIO_CONDUCTOR);
            Conductor conductor = DatosDePrueba.conductor(CONDUCTOR_INTERNO, USUARIO_CONDUCTOR);
            Ruta ruta = Ruta.crear(CONDUCTOR_INTERNO, hoy());
            when(paradas.findByPedidoIdAndEstado(PEDIDO_ID, EstadoParada.PENDIENTE)).thenReturn(Optional.empty());
            when(conductores.findByUsuarioIdForUpdate(USUARIO_CONDUCTOR)).thenReturn(Optional.of(conductor));
            when(gestor.obtenerOCrear(CONDUCTOR_INTERNO)).thenReturn(ruta);
            when(rutas.saveAndFlush(any(Ruta.class))).thenAnswer(inv -> inv.getArgument(0));
            when(conductores.findById(CONDUCTOR_INTERNO)).thenReturn(Optional.of(conductor));

            RutaResponseDTO resultado = rutaService.asignarEnvio(dto);

            assertThat(resultado.conductorId()).isEqualTo(USUARIO_CONDUCTOR);
            assertThat(resultado.paradas()).hasSize(1);
            assertThat(resultado.paradas().get(0).pedidoId()).isEqualTo(PEDIDO_ID);
            verify(registro).registrar(PEDIDO_ID, null, USUARIO_CONDUCTOR, "ASIGNACION", null);
        }

        @Test
        void rechazaUnEnvioYaAsignado() {
            AsignarEnvioRequestDTO dto = new AsignarEnvioRequestDTO(PEDIDO_ID, USUARIO_CONDUCTOR);
            ParadaRuta parada = Ruta.crear(CONDUCTOR_INTERNO, hoy()).agregarParada(PEDIDO_ID);
            when(paradas.findByPedidoIdAndEstado(PEDIDO_ID, EstadoParada.PENDIENTE)).thenReturn(Optional.of(parada));

            assertThatThrownBy(() -> rutaService.asignarEnvio(dto)).isInstanceOf(EnvioYaAsignadoException.class);
        }

        @Test
        void rechazaSiElUsuarioNoEsConductorActivo() {
            AsignarEnvioRequestDTO dto = new AsignarEnvioRequestDTO(PEDIDO_ID, USUARIO_CONDUCTOR);
            when(paradas.findByPedidoIdAndEstado(PEDIDO_ID, EstadoParada.PENDIENTE)).thenReturn(Optional.empty());
            when(conductores.findByUsuarioIdForUpdate(USUARIO_CONDUCTOR)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> rutaService.asignarEnvio(dto)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("reordenarRuta")
    class Reordenar {
        @Test
        void aplicaElOrdenIndicada() {
            Long pedidoB = 20L;
            Long pedidoC = 30L;
            Ruta ruta = rutaConParadas(CONDUCTOR_INTERNO, PEDIDO_ID, pedidoB, pedidoC);
            Conductor conductor = DatosDePrueba.conductor(CONDUCTOR_INTERNO, USUARIO_CONDUCTOR);
            when(rutas.findById(RUTA_ID)).thenReturn(Optional.of(ruta));
            when(rutas.save(any(Ruta.class))).thenAnswer(inv -> inv.getArgument(0));
            when(conductores.findById(CONDUCTOR_INTERNO)).thenReturn(Optional.of(conductor));

            RutaResponseDTO resultado = rutaService.reordenarRuta(RUTA_ID,
                    new ReordenarRutaRequestDTO(List.of(pedidoC, pedidoB, PEDIDO_ID)));

            assertThat(resultado.paradas().get(0).pedidoId()).isEqualTo(pedidoC);
            assertThat(resultado.paradas().get(0).orden()).isEqualTo(1);
        }

        @Test
        void fallaSiLaRutaNoExiste() {
            when(rutas.findById(RUTA_ID)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> rutaService.reordenarRuta(RUTA_ID, new ReordenarRutaRequestDTO(List.of(PEDIDO_ID))))
                    .isInstanceOf(RutaNoEncontradaException.class);
        }
    }

    @Nested
    @DisplayName("reasignarEnvio")
    class Reasignar {
        @Test
        void transfiereElEnvioAlNuevoConductor() {
            ReasignarEnvioRequestDTO dto = new ReasignarEnvioRequestDTO(PEDIDO_ID, USUARIO_NUEVO, "Cambio de zona");
            Conductor origen = DatosDePrueba.conductor(CONDUCTOR_INTERNO, USUARIO_CONDUCTOR);
            Conductor destino = DatosDePrueba.conductor(CONDUCTOR_NUEVO, USUARIO_NUEVO);
            Ruta rutaOrigen = rutaConParadas(CONDUCTOR_INTERNO, PEDIDO_ID);
            ParadaRuta parada = rutaOrigen.getParadas().get(0);
            Ruta rutaDestino = Ruta.crear(CONDUCTOR_NUEVO, hoy());
            when(paradas.findByPedidoIdAndEstado(PEDIDO_ID, EstadoParada.PENDIENTE)).thenReturn(Optional.of(parada));
            when(conductores.findById(CONDUCTOR_INTERNO)).thenReturn(Optional.of(origen));
            when(conductores.findByUsuarioIdForUpdate(USUARIO_NUEVO)).thenReturn(Optional.of(destino));
            when(gestor.obtenerOCrear(CONDUCTOR_NUEVO)).thenReturn(rutaDestino);
            when(envios.obtener(PEDIDO_ID)).thenReturn(DatosDePrueba.pedido(PEDIDO_ID, EstadoPedido.EN_REPARTO));
            when(rutas.saveAndFlush(any(Ruta.class))).thenAnswer(inv -> inv.getArgument(0));
            when(conductores.findById(CONDUCTOR_NUEVO)).thenReturn(Optional.of(destino));

            RutaResponseDTO resultado = rutaService.reasignarEnvio(dto);

            assertThat(resultado.conductorId()).isEqualTo(USUARIO_NUEVO);
            assertThat(resultado.paradas()).hasSize(1);
            assertThat(parada.getEstado()).isEqualTo(EstadoParada.CANCELADA);
            verify(registro).registrar(PEDIDO_ID, USUARIO_CONDUCTOR, USUARIO_NUEVO, "REASIGNACION", dto.motivo());
        }

        @Test
        void fallaSiNoHayParadaPendiente() {
            when(paradas.findByPedidoIdAndEstado(PEDIDO_ID, EstadoParada.PENDIENTE)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> rutaService.reasignarEnvio(new ReasignarEnvioRequestDTO(PEDIDO_ID, USUARIO_NUEVO)))
                    .isInstanceOf(EnvioNoAsignadoException.class);
        }
    }

    @Nested
    @DisplayName("obtenerRutaActivaDeConductor")
    class RutaActiva {
        @Test
        void devuelveLaRutaDelDia() {
            Conductor conductor = DatosDePrueba.conductor(CONDUCTOR_INTERNO, USUARIO_CONDUCTOR);
            Ruta ruta = rutaConParadas(CONDUCTOR_INTERNO, PEDIDO_ID);
            when(conductores.findByUsuarioId(USUARIO_CONDUCTOR)).thenReturn(Optional.of(conductor));
            when(rutas.findByConductorIdAndFecha(CONDUCTOR_INTERNO, hoy())).thenReturn(Optional.of(ruta));
            when(conductores.findById(CONDUCTOR_INTERNO)).thenReturn(Optional.of(conductor));

            RutaResponseDTO resultado = rutaService.obtenerRutaActivaDeConductor(USUARIO_CONDUCTOR);

            assertThat(resultado.conductorId()).isEqualTo(USUARIO_CONDUCTOR);
            assertThat(resultado.paradas()).hasSize(1);
        }

        @Test
        void fallaSiNoHayRutaActiva() {
            Conductor conductor = DatosDePrueba.conductor(CONDUCTOR_INTERNO, USUARIO_CONDUCTOR);
            when(conductores.findByUsuarioId(USUARIO_CONDUCTOR)).thenReturn(Optional.of(conductor));
            when(rutas.findByConductorIdAndFecha(CONDUCTOR_INTERNO, hoy())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> rutaService.obtenerRutaActivaDeConductor(USUARIO_CONDUCTOR))
                    .isInstanceOf(RutaNoEncontradaException.class);
        }
    }

    @Test
    void loteNoAdmiteIdentificadoresDuplicados() {
        AsignacionMasivaRequestDTO dto = new AsignacionMasivaRequestDTO(USUARIO_CONDUCTOR, List.of(PEDIDO_ID, PEDIDO_ID));
        assertThatThrownBy(() -> rutaService.asignarVarios(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("distintos");
        verifyNoInteractions(gestor);
    }
}
