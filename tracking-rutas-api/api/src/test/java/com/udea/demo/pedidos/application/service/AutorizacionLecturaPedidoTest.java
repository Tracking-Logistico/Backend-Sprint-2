package com.udea.demo.pedidos.application.service;

import com.udea.demo.pedidos.domain.model.Pedido;
import com.udea.demo.pedidos.interfaces.services.AccesoPedidoConductorI;
import com.udea.demo.usuarios.application.service.ActorAuthorizationService;
import com.udea.demo.usuarios.domain.model.Rol;
import com.udea.demo.usuarios.domain.model.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AutorizacionLecturaPedido - control de acceso por roles para pedidos")
class AutorizacionLecturaPedidoTest {

    @Mock private ActorAuthorizationService actores;
    @Mock private AccesoPedidoConductorI accesoConductor;

    @InjectMocks
    private AutorizacionLecturaPedido autorizacion;

    private Pedido mockPedido(Long id, Long clienteId) {
        Pedido p = Pedido.builder().build();
        ReflectionTestUtils.setField(p, "id", id);
        ReflectionTestUtils.setField(p, "clienteId", clienteId);
        return p;
    }

    @Test
    @DisplayName("autorizarLectura permite acceso al operador o admin sin restricciones de cliente o conductor")
    void autorizarLectura_operador_accesoPermitido() {
        Usuario operador = Usuario.builder().id(99L).rol(Rol.OPERADOR).build();
        when(actores.actorActual()).thenReturn(operador);

        Pedido p = mockPedido(1L, 10L);
        assertThatCode(() -> autorizacion.autorizarLectura(p)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("autorizarLectura para cliente valida que sea el propietario")
    void autorizarLectura_cliente_validaPropietario() {
        Usuario cliente = Usuario.builder().id(10L).rol(Rol.CLIENTE).build();
        when(actores.actorActual()).thenReturn(cliente);
        when(actores.clienteActualId()).thenReturn(10L);

        Pedido pedidoPropio = mockPedido(1L, 10L);
        assertThatCode(() -> autorizacion.autorizarLectura(pedidoPropio)).doesNotThrowAnyException();

        Pedido pedidoAjeno = mockPedido(2L, 20L);
        assertThatThrownBy(() -> autorizacion.autorizarLectura(pedidoAjeno))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("no pertenece al cliente");
    }

    @Test
    @DisplayName("autorizarLectura para conductor valida asignación activa")
    void autorizarLectura_conductor_validaAsignacionActiva() {
        Usuario conductor = Usuario.builder().id(5L).rol(Rol.CONDUCTOR).build();
        when(actores.actorActual()).thenReturn(conductor);

        Pedido p = mockPedido(1L, 10L);
        when(accesoConductor.tieneAsignacionActiva(1L, 5L)).thenReturn(true);
        assertThatCode(() -> autorizacion.autorizarLectura(p)).doesNotThrowAnyException();

        when(accesoConductor.tieneAsignacionActiva(1L, 5L)).thenReturn(false);
        assertThatThrownBy(() -> autorizacion.autorizarLectura(p))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("no está asignado al conductor");
    }

    @Test
    @DisplayName("autorizarConductor valida si el pedido fue alguna vez asignado al conductor")
    void autorizarConductor_validaAsignacionPrevia() {
        Usuario conductor = Usuario.builder().id(5L).rol(Rol.CONDUCTOR).build();
        when(actores.actorActual()).thenReturn(conductor);

        Pedido p = mockPedido(1L, 10L);
        when(accesoConductor.fueAsignado(1L, 5L)).thenReturn(true);
        assertThatCode(() -> autorizacion.autorizarConductor(p)).doesNotThrowAnyException();

        when(accesoConductor.fueAsignado(1L, 5L)).thenReturn(false);
        assertThatThrownBy(() -> autorizacion.autorizarConductor(p))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("no está asignado al conductor");
    }

    @Test
    @DisplayName("autorizarHistorialConductor exige rol conductor y asignación")
    void autorizarHistorialConductor_validaRolYAsignacion() {
        Usuario operador = Usuario.builder().id(1L).rol(Rol.OPERADOR).build();
        when(actores.actorActual()).thenReturn(operador);

        Pedido p = mockPedido(1L, 10L);
        assertThatThrownBy(() -> autorizacion.autorizarHistorialConductor(p))
                .isInstanceOf(AccessDeniedException.class);

        Usuario conductor = Usuario.builder().id(5L).rol(Rol.CONDUCTOR).build();
        when(actores.actorActual()).thenReturn(conductor);
        when(accesoConductor.fueAsignado(1L, 5L)).thenReturn(true);
        assertThatCode(() -> autorizacion.autorizarHistorialConductor(p)).doesNotThrowAnyException();
    }
}
