package com.udea.demo.rutas.infrastructure.authorization;

import com.udea.demo.pedidos.application.dto.CambiarEstadoLogisticoRequestDTO;
import com.udea.demo.pedidos.application.dto.PedidoResponseDTO;
import com.udea.demo.pedidos.domain.model.EstadoPedido;
import com.udea.demo.pedidos.interfaces.services.PedidoServiceI;
import com.udea.demo.rutas.domain.model.EstadoParada;
import com.udea.demo.rutas.interfaces.persistence.ParadaRutaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Adaptadores de infraestructura de Rutas")
class AdaptadorEnviosParaRutaTest {

    @Mock private PedidoServiceI pedidoService;
    @Mock private ParadaRutaRepository paradaRepository;

    @Test
    @DisplayName("AdaptadorEnviosParaRuta delega correctamente a PedidoService")
    void testAdaptadorEnviosParaRuta() {
        AdaptadorEnviosParaRuta adapter = new AdaptadorEnviosParaRuta(pedidoService);

        PedidoResponseDTO dummy = mock(PedidoResponseDTO.class);
        when(pedidoService.obtener(1L)).thenReturn(dummy);
        assertThat(adapter.obtener(1L)).isEqualTo(dummy);

        Page<PedidoResponseDTO> pagina = new PageImpl<>(List.of(dummy));
        when(pedidoService.listarEnTransito(any(Pageable.class))).thenReturn(pagina);
        assertThat(adapter.listarEnTransito(Pageable.unpaged())).isEqualTo(pagina);

        CambiarEstadoLogisticoRequestDTO cambio = new CambiarEstadoLogisticoRequestDTO(EstadoPedido.EN_REPARTO);
        when(pedidoService.cambiarEstadoLogistico(1L, cambio)).thenReturn(dummy);
        assertThat(adapter.cambiarEstadoLogistico(1L, cambio)).isEqualTo(dummy);
    }

    @Test
    @DisplayName("AccesoPedidoConductor consulta paradas en el repositorio")
    void testAccesoPedidoConductor() {
        AccesoPedidoConductor acceso = new AccesoPedidoConductor(paradaRepository);

        when(paradaRepository.existsByPedidoIdAndRutaConductorUsuarioIdAndEstado(1L, 2L, EstadoParada.PENDIENTE)).thenReturn(true);
        assertThat(acceso.tieneAsignacionActiva(1L, 2L)).isTrue();

        when(paradaRepository.existsByPedidoIdAndRutaConductorUsuarioId(1L, 2L)).thenReturn(true);
        assertThat(acceso.fueAsignado(1L, 2L)).isTrue();
    }
}
