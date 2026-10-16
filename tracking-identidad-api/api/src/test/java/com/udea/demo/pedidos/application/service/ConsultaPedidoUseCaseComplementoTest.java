package com.udea.demo.pedidos.application.service;

import com.udea.demo.pedidos.application.dto.HistorialPedidoResponseDTO;
import com.udea.demo.pedidos.application.dto.PedidoClienteResponseDTO;
import com.udea.demo.pedidos.application.dto.PedidoResponseDTO;
import com.udea.demo.pedidos.application.dto.SeguimientoClienteResponseDTO;
import com.udea.demo.pedidos.domain.model.EstadoPedido;
import com.udea.demo.pedidos.domain.model.HistorialPedido;
import com.udea.demo.pedidos.domain.model.Pedido;
import com.udea.demo.pedidos.domain.model.Prioridad;
import com.udea.demo.pedidos.domain.model.TipoServicio;
import com.udea.demo.pedidos.interfaces.persistence.HistorialPedidoRepository;
import com.udea.demo.pedidos.interfaces.persistence.PedidoRepository;
import com.udea.demo.usuarios.application.service.ActorAuthorizationService;
import com.udea.demo.usuarios.domain.model.Rol;
import com.udea.demo.usuarios.domain.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConsultaPedidoUseCaseComplementoTest - métodos de listados, filtros y conductor")
class ConsultaPedidoUseCaseComplementoTest {

    @Mock private PedidoRepository pedidos;
    @Mock private HistorialPedidoRepository historial;
    @Mock private ActorAuthorizationService actores;
    @Mock private SeguimientoClienteService seguimientoCliente;
    @Mock private AutorizacionLecturaPedido autorizacion;
    @Spy private PedidoMapeador mapeador = new PedidoMapeador();

    @InjectMocks
    private ConsultaPedidoUseCase useCase;

    private Pedido mockPedido(Long id, EstadoPedido estado) {
        Pedido p = Pedido.recibir(10L, "Dir O", "Bogotá", "1101",
                "Dir D", "Bogotá", "1102", "Paquete",
                1.0, 10.0, 10.0, 10.0, TipoServicio.ESTANDAR, "PED-" + id,
                Prioridad.MEDIA, "Dest", "300", "Rem", "rem@test.com", "300");
        ReflectionTestUtils.setField(p, "id", id);
        ReflectionTestUtils.setField(p, "estado", estado);
        return p;
    }

    @Test
    @DisplayName("listarMios consulta repositorio con cliente y mapea")
    void listarMios_exitoso() {
        Usuario actor = Usuario.builder().id(10L).email("cliente@test.com").rol(Rol.CLIENTE).build();
        when(actores.actorActual()).thenReturn(actor);
        when(actores.clienteActualId()).thenReturn(10L);

        Pedido p = mockPedido(1L, EstadoPedido.CREADO);
        when(pedidos.findPedidosDeCliente(eq(10L), eq("cliente@test.com"), any(), eq(EstadoPedido.CREADO), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(p)));

        Page<PedidoClienteResponseDTO> res = useCase.listarMios(EstadoPedido.CREADO, null, null, PageRequest.of(0, 10));

        assertThat(res.getContent()).hasSize(1);
        assertThat(res.getContent().get(0).id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("listarValidados y listarActivables mapean correctamente")
    void listarValidadosYActivables() {
        Pedido p = mockPedido(2L, EstadoPedido.SOLICITADO);
        when(pedidos.findByEstadoAndFechaValidacionIsNotNullOrderByFechaCreacionAsc(eq(EstadoPedido.SOLICITADO), any()))
                .thenReturn(new PageImpl<>(List.of(p)));
        when(pedidos.findActivables(eq(EstadoPedido.SOLICITADO), eq(EstadoPedido.CREADO), any()))
                .thenReturn(new PageImpl<>(List.of(p)));

        Page<PedidoResponseDTO> validados = useCase.listarValidados(PageRequest.of(0, 5));
        Page<PedidoResponseDTO> activables = useCase.listarActivables(PageRequest.of(0, 5));

        assertThat(validados.getContent()).hasSize(1);
        assertThat(activables.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("listarDespachos filtra pedidos SOLICITADO que no tengan validación")
    void listarDespachos_filtraSinValidacion() {
        Pedido p1 = mockPedido(1L, EstadoPedido.SOLICITADO); // Sin validación
        Pedido p2 = mockPedido(2L, EstadoPedido.SOLICITADO);
        ReflectionTestUtils.setField(p2, "fechaValidacion", LocalDateTime.now()); // Con validación
        Pedido p3 = mockPedido(3L, EstadoPedido.RECIBIDO_EN_ORIGEN);

        when(pedidos.findByEstadoInOrderByFechaCreacionAsc(any())).thenReturn(List.of(p1, p2, p3));

        List<PedidoResponseDTO> despachos = useCase.listarDespachos();

        assertThat(despachos).hasSize(2);
        assertThat(despachos.get(0).id()).isEqualTo(2L);
        assertThat(despachos.get(1).id()).isEqualTo(3L);
    }

    @Test
    @DisplayName("listarEnTransito pagina pedidos recibidos en origen o transito")
    void listarEnTransito_exitoso() {
        Pedido p = mockPedido(4L, EstadoPedido.EN_TRANSITO);
        when(pedidos.findByEstadoInOrderByFechaCreacionAsc(any(), any())).thenReturn(new PageImpl<>(List.of(p)));

        Page<PedidoResponseDTO> res = useCase.listarEnTransito(PageRequest.of(0, 10));

        assertThat(res.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("obtenerPorIds retorna mapa por ID o mapa vacío con colección vacía")
    void obtenerPorIds_casos() {
        assertThat(useCase.obtenerPorIds(null)).isEmpty();
        assertThat(useCase.obtenerPorIds(Collections.emptyList())).isEmpty();

        Pedido p1 = mockPedido(10L, EstadoPedido.EN_REPARTO);
        Pedido p2 = mockPedido(20L, EstadoPedido.ENTREGADO);
        when(pedidos.findByIdIn(List.of(10L, 20L))).thenReturn(List.of(p1, p2));

        Map<Long, PedidoResponseDTO> mapa = useCase.obtenerPorIds(List.of(10L, 20L));

        assertThat(mapa).containsKeys(10L, 20L);
        assertThat(mapa.get(10L).id()).isEqualTo(10L);
    }

    @Test
    @DisplayName("obtenerParaConductor e historialParaConductor validan permisos y retornan datos")
    void paraConductor_exitoso() {
        Pedido p = mockPedido(30L, EstadoPedido.EN_REPARTO);
        when(pedidos.findById(30L)).thenReturn(Optional.of(p));

        HistorialPedido h = HistorialPedido.builder().id(1L).pedidoId(30L).usuarioId(5L)
                .tipoEvento("CHECKPOINT").detalle("En ruta").fecha(LocalDateTime.now()).build();
        when(historial.findByPedidoIdOrderByFechaAsc(30L)).thenReturn(List.of(h));

        PedidoResponseDTO pedRes = useCase.obtenerParaConductor(30L);
        List<HistorialPedidoResponseDTO> histRes = useCase.historialParaConductor(30L);

        verify(autorizacion).autorizarConductor(p);
        verify(autorizacion).autorizarHistorialConductor(p);
        assertThat(pedRes.id()).isEqualTo(30L);
        assertThat(histRes).hasSize(1);
    }

    @Test
    @DisplayName("obtenerSeguimientoCliente delega al servicio correspondiente")
    void obtenerSeguimientoCliente_delega() {
        SeguimientoClienteResponseDTO mockDto = org.mockito.Mockito.mock(SeguimientoClienteResponseDTO.class);
        when(seguimientoCliente.obtener("LT123")).thenReturn(mockDto);

        assertThat(useCase.obtenerSeguimientoCliente("LT123")).isEqualTo(mockDto);
    }
}
