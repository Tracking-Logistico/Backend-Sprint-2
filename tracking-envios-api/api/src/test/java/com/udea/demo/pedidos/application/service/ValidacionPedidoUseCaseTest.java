package com.udea.demo.pedidos.application.service;

import com.udea.demo.pedidos.application.dto.PedidoResponseDTO;
import com.udea.demo.pedidos.application.dto.ValidarPedidoRequestDTO;
import com.udea.demo.pedidos.domain.exception.PedidoNoEncontradoException;
import com.udea.demo.pedidos.domain.model.EstadoPedido;
import com.udea.demo.pedidos.domain.model.Pedido;
import com.udea.demo.pedidos.domain.model.Prioridad;
import com.udea.demo.pedidos.domain.model.TipoServicio;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ValidacionPedidoUseCase - validación, rechazo y corrección con datos tipados")
class ValidacionPedidoUseCaseTest {

    @Mock private PedidoRepository pedidos;
    @Mock private ActorAuthorizationService actores;
    @Mock private RegistroHistorialPedido historial;

    private ValidacionPedidoUseCase useCase;
    private final PedidoMapeador mapeador = new PedidoMapeador();

    private static final Long OPERADOR_ID = 45L;
    private static final Long PEDIDO_ID = 200L;

    @BeforeEach
    void setUp() {
        useCase = new ValidacionPedidoUseCase(pedidos, actores, historial, mapeador);

        Usuario operador = Usuario.builder().id(OPERADOR_ID).nombre("Operador Logístico")
                .email("operador@logistica.com").rol(Rol.OPERADOR).activo(true).build();
        org.mockito.Mockito.lenient().when(actores.actorActual()).thenReturn(operador);
        org.mockito.Mockito.lenient().when(actores.operadorActualId()).thenReturn(OPERADOR_ID);
    }

    private Pedido pedidoSolicitado() {
        Pedido p = Pedido.recibir(
                10L, "Carrera 7 #71-21, Bogotá", "Bogotá", "110111",
                "Calle 45 #12-30, Bogotá", "Bogotá", "110111",
                "Caja de componentes", 2.0, 20.0, 15.0, 10.0,
                TipoServicio.EXPRESS, "PED-20261015-0001", Prioridad.MEDIA,
                "Destinatario", "+573001234567", "Remitente",
                "remitente@test.com", "+573109876543");
        org.springframework.test.util.ReflectionTestUtils.setField(p, "id", PEDIDO_ID);
        return p;
    }

    @Test
    @DisplayName("validar() aprueba el pedido y registra evento PEDIDO_VALIDADO")
    void validar_aprobarExitoso() {
        Pedido pedido = pedidoSolicitado();
        when(pedidos.findByIdForUpdate(PEDIDO_ID)).thenReturn(Optional.of(pedido));
        when(pedidos.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        ValidarPedidoRequestDTO dto = new ValidarPedidoRequestDTO(
                true, Prioridad.ALTA, "Dirección y peso confirmados",
                "Ajuste por solicitud de cliente express", null, false);

        PedidoResponseDTO respuesta = useCase.validar(PEDIDO_ID, dto);

        assertThat(respuesta.estado()).isEqualTo(EstadoPedido.SOLICITADO);
        assertThat(respuesta.prioridadConfirmada()).isEqualTo(Prioridad.ALTA);
        assertThat(respuesta.operadorValidadorId()).isEqualTo(OPERADOR_ID);
        verify(historial).registrar(eq(PEDIDO_ID), eq(OPERADOR_ID), eq("PEDIDO_VALIDADO"), org.mockito.ArgumentMatchers.isNull(), any(), any());
    }

    @Test
    @DisplayName("validar() rechaza el pedido si aprobar es false y hay observaciones")
    void validar_rechazarExitoso() {
        Pedido pedido = pedidoSolicitado();
        when(pedidos.findByIdForUpdate(PEDIDO_ID)).thenReturn(Optional.of(pedido));
        when(pedidos.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        ValidarPedidoRequestDTO dto = new ValidarPedidoRequestDTO(
                false, null, "Material peligroso no permitido para transporte express",
                null, null, false);

        PedidoResponseDTO respuesta = useCase.validar(PEDIDO_ID, dto);

        assertThat(respuesta.estado()).isEqualTo(EstadoPedido.RECHAZADO);
        verify(historial).registrar(eq(PEDIDO_ID), eq(OPERADOR_ID), eq("PEDIDO_RECHAZADO"), org.mockito.ArgumentMatchers.isNull(), eq("Material peligroso no permitido para transporte express"), any());
    }

    @Test
    @DisplayName("validar() solicita corrección con campoObservado obligatorio")
    void validar_solicitarCorreccion() {
        Pedido pedido = pedidoSolicitado();
        when(pedidos.findByIdForUpdate(PEDIDO_ID)).thenReturn(Optional.of(pedido));
        when(pedidos.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        ValidarPedidoRequestDTO dto = new ValidarPedidoRequestDTO(
                null, null, "Por favor actualizar número de apartamento",
                null, "direccionDestino", true);

        PedidoResponseDTO respuesta = useCase.validar(PEDIDO_ID, dto);

        assertThat(respuesta.estado()).isEqualTo(EstadoPedido.CORRECCION_SOLICITADA);
        verify(historial).registrar(eq(PEDIDO_ID), eq(OPERADOR_ID), eq("CORRECCION_SOLICITADA"), eq("direccionDestino"), eq("Por favor actualizar número de apartamento"), any());
    }

    @Test
    @DisplayName("validar() sobre pedido inexistente lanza PedidoNoEncontradoException")
    void validar_inexistente() {
        when(pedidos.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        ValidarPedidoRequestDTO dto = new ValidarPedidoRequestDTO(
                true, Prioridad.MEDIA, "ok", null, null, false);

        assertThatThrownBy(() -> useCase.validar(999L, dto))
                .isInstanceOf(PedidoNoEncontradoException.class);
    }
}
