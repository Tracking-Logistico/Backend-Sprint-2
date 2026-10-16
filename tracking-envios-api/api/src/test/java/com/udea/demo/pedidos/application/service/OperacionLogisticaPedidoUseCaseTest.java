package com.udea.demo.pedidos.application.service;

import com.udea.demo.pedidos.application.dto.CambiarEstadoLogisticoRequestDTO;
import com.udea.demo.pedidos.application.dto.EtiquetaEnvioResponseDTO;
import com.udea.demo.pedidos.application.dto.PedidoResponseDTO;
import com.udea.demo.pedidos.domain.exception.TrackingNoActivoException;
import com.udea.demo.pedidos.domain.model.EstadoPedido;
import com.udea.demo.pedidos.domain.model.Pedido;
import com.udea.demo.pedidos.domain.model.Prioridad;
import com.udea.demo.pedidos.domain.model.TipoServicio;
import com.udea.demo.pedidos.domain.service.GeneradorEtiqueta;
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
@DisplayName("OperacionLogisticaPedidoUseCase - tracking, estados y etiquetas con datos tipados")
class OperacionLogisticaPedidoUseCaseTest {

    @Mock private PedidoRepository pedidos;
    @Mock private ActorAuthorizationService actores;
    @Mock private GeneradorNumeroTracking numerosTracking;
    @Mock private GeneradorEtiqueta etiquetas;
    @Mock private RegistroHistorialPedido historial;

    private OperacionLogisticaPedidoUseCase useCase;
    private final PedidoMapeador mapeador = new PedidoMapeador();

    private static final Long OPERADOR_ID = 45L;
    private static final Long PEDIDO_ID = 300L;
    private static final String NUMERO_TRACKING = "LT1234567890";

    @BeforeEach
    void setUp() {
        useCase = new OperacionLogisticaPedidoUseCase(pedidos, actores, numerosTracking,
                etiquetas, historial, mapeador);

        Usuario operador = Usuario.builder().id(OPERADOR_ID).nombre("Operador Logístico")
                .email("operador@logistica.com").rol(Rol.OPERADOR).activo(true).build();
        org.mockito.Mockito.lenient().when(actores.actorActual()).thenReturn(operador);
        org.mockito.Mockito.lenient().when(actores.operadorActualId()).thenReturn(OPERADOR_ID);
    }

    private Pedido pedidoValidado() {
        Pedido p = Pedido.recibir(
                10L, "Carrera 7 #71-21, Bogotá", "Bogotá", "110111",
                "Calle 45 #12-30, Bogotá", "Bogotá", "110111",
                "Caja de herramientas", 3.0, 30.0, 20.0, 15.0,
                TipoServicio.ESTANDAR, "PED-20261015-0002", Prioridad.MEDIA,
                "Destinatario", "+573001234567", "Remitente",
                "remitente@test.com", "+573109876543");
        p.validar(true, Prioridad.MEDIA, "Validado", OPERADOR_ID);
        org.springframework.test.util.ReflectionTestUtils.setField(p, "id", PEDIDO_ID);
        return p;
    }

    @Test
    @DisplayName("activarTracking() asigna número de tracking único y transiciona a CREADO")
    void activarTracking_exitoso() {
        Pedido pedido = pedidoValidado();
        when(pedidos.findByIdForUpdate(PEDIDO_ID)).thenReturn(Optional.of(pedido));
        when(numerosTracking.generar()).thenReturn(NUMERO_TRACKING);
        when(pedidos.existsByNumeroTracking(NUMERO_TRACKING)).thenReturn(false);
        when(pedidos.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        PedidoResponseDTO respuesta = useCase.activarTracking(PEDIDO_ID);

        assertThat(respuesta.estado()).isEqualTo(EstadoPedido.CREADO);
        assertThat(respuesta.numeroTracking()).isEqualTo(NUMERO_TRACKING);
        verify(historial).registrar(eq(PEDIDO_ID), eq(OPERADOR_ID), eq("TRACKING_ACTIVADO"), org.mockito.ArgumentMatchers.isNull(), eq(NUMERO_TRACKING), any());
    }

    @Test
    @DisplayName("cambiarEstadoLogistico() avanza el estado y registra en historial")
    void cambiarEstadoLogistico_exitoso() {
        Pedido pedido = pedidoValidado();
        pedido.activarTracking(NUMERO_TRACKING);
        // cambiarEstadoLogistico usa buscar() → findById (no findByIdForUpdate)
        when(pedidos.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));
        when(pedidos.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        CambiarEstadoLogisticoRequestDTO dto = new CambiarEstadoLogisticoRequestDTO(EstadoPedido.RECIBIDO_EN_ORIGEN);
        PedidoResponseDTO respuesta = useCase.cambiarEstadoLogistico(PEDIDO_ID, dto);

        assertThat(respuesta.estado()).isEqualTo(EstadoPedido.RECIBIDO_EN_ORIGEN);
        verify(historial).registrar(eq(PEDIDO_ID), eq(OPERADOR_ID), eq("ESTADO_LOGISTICO"), org.mockito.ArgumentMatchers.isNull(), eq("RECIBIDO_EN_ORIGEN"), any());
    }

    @Test
    @DisplayName("generarEtiqueta() genera contenido PDF en Base64 cuando tiene tracking activo")
    void generarEtiqueta_exitoso() {
        Pedido pedido = pedidoValidado();
        pedido.activarTracking(NUMERO_TRACKING);
        when(pedidos.findByIdForUpdate(PEDIDO_ID)).thenReturn(Optional.of(pedido));
        when(etiquetas.generar(pedido)).thenReturn("JVBERi0xLjQKJ...");

        EtiquetaEnvioResponseDTO respuesta = useCase.generarEtiqueta(PEDIDO_ID);

        assertThat(respuesta.numeroTracking()).isEqualTo(NUMERO_TRACKING);
        assertThat(respuesta.contenido()).isEqualTo("JVBERi0xLjQKJ...");
        assertThat(pedido.getEtiquetaImpresa()).isTrue();
    }

    @Test
    @DisplayName("generarEtiqueta() sin tracking lanza TrackingNoActivoException")
    void generarEtiqueta_sinTracking_lanzaExcepcion() {
        Pedido pedido = pedidoValidado();
        when(pedidos.findByIdForUpdate(PEDIDO_ID)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> useCase.generarEtiqueta(PEDIDO_ID))
                .isInstanceOf(TrackingNoActivoException.class);
    }
}
