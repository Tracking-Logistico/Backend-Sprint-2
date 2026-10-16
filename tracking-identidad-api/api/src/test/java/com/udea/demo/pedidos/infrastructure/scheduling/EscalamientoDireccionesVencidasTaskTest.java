package com.udea.demo.pedidos.infrastructure.scheduling;

import com.udea.demo.pedidos.application.service.VerificacionDireccionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EscalamientoDireccionesVencidasTask - tarea programada de escalamiento")
class EscalamientoDireccionesVencidasTaskTest {

    @Mock private VerificacionDireccionService verificacion;

    @InjectMocks
    private EscalamientoDireccionesVencidasTask task;

    @Test
    @DisplayName("escalar procesa lista de pendientes y maneja excepciones individuales sin detener el lote")
    void escalar_procesaLoteYManejaErrores() {
        when(verificacion.pendientesDeEscalar(any(LocalDateTime.class))).thenReturn(List.of(10L, 20L, 30L));
        when(verificacion.escalarVencida(eq(10L), any(LocalDateTime.class))).thenReturn(true);
        when(verificacion.escalarVencida(eq(20L), any(LocalDateTime.class))).thenThrow(new RuntimeException("Fallo BD"));
        when(verificacion.escalarVencida(eq(30L), any(LocalDateTime.class))).thenReturn(false);

        task.escalar();

        verify(verificacion).escalarVencida(eq(10L), any(LocalDateTime.class));
        verify(verificacion).escalarVencida(eq(20L), any(LocalDateTime.class));
        verify(verificacion).escalarVencida(eq(30L), any(LocalDateTime.class));
    }
}
