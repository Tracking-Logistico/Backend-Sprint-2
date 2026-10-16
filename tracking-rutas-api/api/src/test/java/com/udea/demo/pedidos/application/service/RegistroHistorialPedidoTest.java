package com.udea.demo.pedidos.application.service;

import com.udea.demo.pedidos.domain.model.HistorialPedido;
import com.udea.demo.pedidos.interfaces.persistence.HistorialPedidoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegistroHistorialPedido - registro de auditoría de línea de tiempo")
class RegistroHistorialPedidoTest {

    @Mock private HistorialPedidoRepository historialRepository;

    @InjectMocks
    private RegistroHistorialPedido service;

    @Test
    @DisplayName("Guarda entidad HistorialPedido con todos los campos especificados")
    void registrar_guardaEntidad() {
        LocalDateTime ahora = LocalDateTime.now();
        service.registrar(10L, 5L, RegistroHistorialPedido.ESTADO_LOGISTICO, "estado", "Cambio a EN_TRANSITO", ahora);

        ArgumentCaptor<HistorialPedido> captor = ArgumentCaptor.forClass(HistorialPedido.class);
        verify(historialRepository).save(captor.capture());

        HistorialPedido guardado = captor.getValue();
        assertThat(guardado.getPedidoId()).isEqualTo(10L);
        assertThat(guardado.getUsuarioId()).isEqualTo(5L);
        assertThat(guardado.getTipoEvento()).isEqualTo(RegistroHistorialPedido.ESTADO_LOGISTICO);
        assertThat(guardado.getCampoObservado()).isEqualTo("estado");
        assertThat(guardado.getDetalle()).isEqualTo("Cambio a EN_TRANSITO");
        assertThat(guardado.getFecha()).isEqualTo(ahora);
    }
}
