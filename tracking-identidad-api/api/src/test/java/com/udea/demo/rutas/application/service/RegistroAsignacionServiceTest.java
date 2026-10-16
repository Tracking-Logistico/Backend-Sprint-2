package com.udea.demo.rutas.application.service;

import com.udea.demo.rutas.application.dto.HistorialAsignacionDTO;
import com.udea.demo.rutas.application.dto.NotificacionRutaDTO;
import com.udea.demo.usuarios.application.service.ActorAuthorizationService;
import com.udea.demo.usuarios.domain.model.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegistroAsignacionService - historial de asignaciones y notificaciones al conductor")
class RegistroAsignacionServiceTest {

    @Mock private JdbcTemplate jdbc;
    @Mock private ActorAuthorizationService actores;

    @InjectMocks
    private RegistroAsignacionService service;

    @Test
    @DisplayName("registrar inserta en historial_asignaciones y notificaciones_conductor")
    void registrar_insertaHistorialYNotificacion() {
        Usuario operador = Usuario.builder().id(99L).build();
        when(actores.actorActual()).thenReturn(operador);

        service.registrar(10L, 1L, 2L, "ASIGNACION", "Nueva carga");

        verify(jdbc).update(startsWith("INSERT INTO historial_asignaciones"),
                eq(10L), eq(1L), eq(2L), eq(99L), eq("ASIGNACION"), eq("Nueva carga"));
        verify(jdbc).update(startsWith("INSERT INTO notificaciones_conductor"),
                eq(2L), eq(10L), contains("nuevo envío asignado"));
    }

    @Test
    @SuppressWarnings("rawtypes")
    @DisplayName("misNotificaciones consulta notificaciones del conductor autenticado")
    void misNotificaciones_consultaPorConductor() {
        when(actores.conductorActualUsuarioId()).thenReturn(5L);
        List<NotificacionRutaDTO> esperadas = List.of(
                new NotificacionRutaDTO(1L, 10L, "Mensaje", LocalDateTime.now(), false));
        when(jdbc.query(anyString(), any(RowMapper.class), eq(5L))).thenReturn(esperadas);

        List<NotificacionRutaDTO> resultado = service.misNotificaciones();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).mensaje()).isEqualTo("Mensaje");
    }

    @Test
    @DisplayName("marcarLeida actualiza a true si pertenece al conductor")
    void marcarLeida_exitoso() {
        when(actores.conductorActualUsuarioId()).thenReturn(5L);
        when(jdbc.update(anyString(), eq(1L), eq(5L))).thenReturn(1);

        service.marcarLeida(1L);

        verify(jdbc).update(anyString(), eq(1L), eq(5L));
    }

    @Test
    @DisplayName("marcarLeida lanza AccessDeniedException si no existe notificación propia")
    void marcarLeida_noPropia_lanzaExcepcion() {
        when(actores.conductorActualUsuarioId()).thenReturn(5L);
        when(jdbc.update(anyString(), eq(2L), eq(5L))).thenReturn(0);

        assertThatThrownBy(() -> service.marcarLeida(2L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @SuppressWarnings("rawtypes")
    @DisplayName("historial consulta registros de asignación del pedido")
    void historial_consultaPorPedido() {
        when(actores.operadorActualId()).thenReturn(1L);
        List<HistorialAsignacionDTO> esperados = List.of(
                new HistorialAsignacionDTO(1L, 10L, null, 5L, 1L, "ASIGNACION", "Inicio", LocalDateTime.now()));
        when(jdbc.query(anyString(), any(RowMapper.class), eq(10L))).thenReturn(esperados);

        List<HistorialAsignacionDTO> resultado = service.historial(10L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).accion()).isEqualTo("ASIGNACION");
    }
}
