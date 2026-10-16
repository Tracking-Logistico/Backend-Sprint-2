package com.udea.demo.pedidos.application.service;

import com.udea.demo.pedidos.application.dto.HistorialPedidoResponseDTO;
import com.udea.demo.pedidos.application.dto.PedidoResponseDTO;
import com.udea.demo.pedidos.domain.exception.PedidoNoEncontradoException;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConsultaPedidoUseCase - lecturas y listados con datos tipados")
class ConsultaPedidoUseCaseTest {

    @Mock private PedidoRepository pedidos;
    @Mock private HistorialPedidoRepository historial;
    @Mock private ActorAuthorizationService actores;
    @Mock private SeguimientoClienteService seguimientoCliente;
    @Mock private AutorizacionLecturaPedido autorizacion;

    private ConsultaPedidoUseCase useCase;
    private final PedidoMapeador mapeador = new PedidoMapeador();

    private static final Long PEDIDO_ID = 400L;
    private static final String NUMERO_TRACKING = "LT9876543210";

    @BeforeEach
    void setUp() {
        useCase = new ConsultaPedidoUseCase(pedidos, historial, actores,
                seguimientoCliente, autorizacion, mapeador);

        Usuario usuario = Usuario.builder().id(10L).nombre("Cliente Consulta")
                .email("cliente@consulta.com").rol(Rol.CLIENTE).activo(true).build();
        org.mockito.Mockito.lenient().when(actores.actorActual()).thenReturn(usuario);
        org.mockito.Mockito.lenient().when(actores.clienteActualId()).thenReturn(10L);
    }

    private Pedido pedidoPrueba() {
        Pedido p = Pedido.recibir(
                10L, "Carrera 7 #71-21, Bogotá", "Bogotá", "110111",
                "Calle 45 #12-30, Bogotá", "Bogotá", "110111",
                "Caja de libros", 1.5, 20.0, 15.0, 5.0,
                TipoServicio.ESTANDAR, "PED-20261015-0003", Prioridad.BAJA,
                "Destinatario", "+573001234567", "Remitente",
                "cliente@consulta.com", "+573109876543");
        p.validar(true, Prioridad.BAJA, "Ok", 99L);
        p.activarTracking(NUMERO_TRACKING);
        org.springframework.test.util.ReflectionTestUtils.setField(p, "id", PEDIDO_ID);
        return p;
    }

    @Test
    @DisplayName("obtener() verifica permisos y retorna PedidoResponseDTO")
    void obtener_exitoso() {
        Pedido pedido = pedidoPrueba();
        when(pedidos.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));

        PedidoResponseDTO respuesta = useCase.obtener(PEDIDO_ID);

        verify(autorizacion).autorizarLectura(pedido);
        assertThat(respuesta.id()).isEqualTo(PEDIDO_ID);
        assertThat(respuesta.numeroTracking()).isEqualTo(NUMERO_TRACKING);
    }

    @Test
    @DisplayName("obtener() de ID inexistente lanza PedidoNoEncontradoException")
    void obtener_inexistente() {
        when(pedidos.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.obtener(999L))
                .isInstanceOf(PedidoNoEncontradoException.class);
    }

    @Test
    @DisplayName("obtenerPorTracking() verifica permisos y mapea datos")
    void obtenerPorTracking_exitoso() {
        Pedido pedido = pedidoPrueba();
        when(pedidos.findByNumeroTracking(NUMERO_TRACKING)).thenReturn(Optional.of(pedido));

        PedidoResponseDTO respuesta = useCase.obtenerPorTracking(NUMERO_TRACKING);

        verify(autorizacion).autorizarLectura(pedido);
        assertThat(respuesta.id()).isEqualTo(PEDIDO_ID);
    }

    @Test
    @DisplayName("listarPendientes() consulta pedidos en estado SOLICITADO y CORRECCION_SOLICITADA")
    void listarPendientes_exitoso() {
        Pageable pageable = PageRequest.of(0, 10);
        Pedido pedido = pedidoPrueba();
        Page<Pedido> paginaPedidos = new PageImpl<>(List.of(pedido));
        when(pedidos.findPendientes(EstadoPedido.SOLICITADO, EstadoPedido.CORRECCION_SOLICITADA, pageable))
                .thenReturn(paginaPedidos);

        Page<PedidoResponseDTO> resultado = useCase.listarPendientes(pageable);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).id()).isEqualTo(PEDIDO_ID);
    }

    @Test
    @DisplayName("historial() retorna la lista de eventos en formato HistorialPedidoResponseDTO")
    void historial_exitoso() {
        Pedido pedido = pedidoPrueba();
        when(pedidos.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));
        HistorialPedido h1 = HistorialPedido.builder().id(1L).pedidoId(PEDIDO_ID).usuarioId(10L)
                .tipoEvento("PEDIDO_SOLICITADO").detalle("Creado").fecha(LocalDateTime.now()).build();
        when(historial.findByPedidoIdOrderByFechaAsc(PEDIDO_ID)).thenReturn(List.of(h1));

        List<HistorialPedidoResponseDTO> lista = useCase.historial(PEDIDO_ID);

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).tipoEvento()).isEqualTo("PEDIDO_SOLICITADO");
    }
}
