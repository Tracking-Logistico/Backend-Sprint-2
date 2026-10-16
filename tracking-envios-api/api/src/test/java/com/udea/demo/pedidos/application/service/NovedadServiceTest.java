package com.udea.demo.pedidos.application.service;

import com.udea.demo.pedidos.application.dto.NovedadResponseDTO;
import com.udea.demo.pedidos.domain.model.HistorialPedido;
import com.udea.demo.pedidos.interfaces.persistence.HistorialPedidoRepository;
import com.udea.demo.usuarios.application.service.ActorAuthorizationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NovedadService - listado de movimientos logísticos recientes para operadores")
class NovedadServiceTest {

    @Mock private HistorialPedidoRepository historial;
    @Mock private ActorAuthorizationService actores;

    @InjectMocks
    private NovedadService service;

    @Test
    @DisplayName("listar verifica rol operador y mapea eventos de historial a DTOs")
    void listar_exitoso() {
        when(actores.operadorActualId()).thenReturn(99L);
        Pageable pageable = PageRequest.of(0, 10);

        HistorialPedido h1 = HistorialPedido.builder().id(1L).pedidoId(10L).usuarioId(5L)
                .tipoEvento(RegistroHistorialPedido.CHECKPOINT).detalle("Punto A")
                .fecha(LocalDateTime.now()).build();
        Page<HistorialPedido> pagina = new PageImpl<>(List.of(h1));

        when(historial.findByTipoEventoInOrderByFechaDescIdDesc(eq(NovedadService.TIPOS_NOVEDAD), any(Pageable.class)))
                .thenReturn(pagina);

        Page<NovedadResponseDTO> res = service.listar(pageable);

        verify(actores).operadorActualId();
        assertThat(res.getContent()).hasSize(1);
        assertThat(res.getContent().get(0).id()).isEqualTo(1L);
        assertThat(res.getContent().get(0).pedidoId()).isEqualTo(10L);
        assertThat(res.getContent().get(0).tipoEvento()).isEqualTo(RegistroHistorialPedido.CHECKPOINT);
    }
}
