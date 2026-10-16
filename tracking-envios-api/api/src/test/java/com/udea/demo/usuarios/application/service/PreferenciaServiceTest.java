package com.udea.demo.usuarios.application.service;

import com.udea.demo.usuarios.application.dto.ActualizarPreferenciaRequestDTO;
import com.udea.demo.usuarios.application.dto.PreferenciaResponseDTO;
import com.udea.demo.usuarios.domain.model.CanalNotificacion;
import com.udea.demo.usuarios.domain.model.PreferenciaNotificacion;
import com.udea.demo.usuarios.interfaces.persistence.PreferenciaNotificacionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PreferenciaService - consulta y actualización con tipos del dominio")
class PreferenciaServiceTest {

    @Mock
    private PreferenciaNotificacionRepository repository;
    @Mock
    private ActorAuthorizationService actores;
    @InjectMocks
    private PreferenciaService service;

    @Test
    @DisplayName("Sin preferencia persistida retorna EMAIL como canal por defecto")
    void obtener_sinPreferencia_retornaEmail() {
        when(actores.clienteActualId()).thenReturn(15L);
        when(repository.findById(15L)).thenReturn(Optional.empty());

        PreferenciaResponseDTO respuesta = service.obtener();

        assertThat(respuesta.canal()).isEqualTo(CanalNotificacion.EMAIL);
        assertThat(respuesta.telefonoSms()).isNull();
        assertThat(respuesta.fechaActualizacion()).isNull();
    }

    @Test
    @DisplayName("Con preferencia persistida mapea canal, teléfono SMS y fecha de actualización")
    void obtener_conPreferencia_mapeaEntidad() {
        PreferenciaNotificacion preferencia = new PreferenciaNotificacion(15L);
        preferencia.actualizar(CanalNotificacion.SMS, "+573001234567");
        when(actores.clienteActualId()).thenReturn(15L);
        when(repository.findById(15L)).thenReturn(Optional.of(preferencia));

        PreferenciaResponseDTO respuesta = service.obtener();

        assertThat(respuesta.canal()).isEqualTo(CanalNotificacion.SMS);
        assertThat(respuesta.telefonoSms()).isEqualTo("+573001234567");
        assertThat(respuesta.fechaActualizacion()).isEqualTo(preferencia.getFechaActualizacion());
    }

    @Test
    @DisplayName("Actualizar persiste el canal AMBOS y el teléfono SMS del DTO")
    void actualizar_creaOActualizaPreferencia() {
        ActualizarPreferenciaRequestDTO solicitud =
                new ActualizarPreferenciaRequestDTO(CanalNotificacion.AMBOS, "+573104443322");
        when(actores.clienteActualId()).thenReturn(15L);
        when(repository.findById(15L)).thenReturn(Optional.empty());
        when(repository.save(any(PreferenciaNotificacion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PreferenciaResponseDTO respuesta = service.actualizar(solicitud);

        assertThat(respuesta.canal()).isEqualTo(CanalNotificacion.AMBOS);
        assertThat(respuesta.telefonoSms()).isEqualTo("+573104443322");
        assertThat(respuesta.fechaActualizacion()).isNotNull();
        verify(repository).save(any(PreferenciaNotificacion.class));
    }
}
