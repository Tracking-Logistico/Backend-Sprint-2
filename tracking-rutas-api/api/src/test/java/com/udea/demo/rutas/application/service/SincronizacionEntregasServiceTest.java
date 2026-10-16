package com.udea.demo.rutas.application.service;

import com.udea.demo.rutas.application.dto.ResultadoEntregaRequestDTO;
import com.udea.demo.rutas.application.dto.ResultadoEntregaResponseDTO;
import com.udea.demo.rutas.application.dto.SincronizacionEntregasDTO;
import com.udea.demo.usuarios.application.service.ActorAuthorizationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SincronizacionEntregasService - sincronización en lote de entregas offline")
class SincronizacionEntregasServiceTest {

    @Mock private RegistroResultadoEntregaService registros;
    @Mock private ActorAuthorizationService actores;

    @InjectMocks
    private SincronizacionEntregasService service;

    @Test
    @DisplayName("Sincroniza lote con eventos exitosos y eventos rechazados por error de validación")
    void sincronizar_procesaLoteMixto() {
        when(actores.conductorActualUsuarioId()).thenReturn(5L);

        ResultadoEntregaRequestDTO evt1 = new ResultadoEntregaRequestDTO(
                ResultadoEntregaRequestDTO.Resultado.ENTREGADO,
                null, null, 6.2, -75.5, OffsetDateTime.now(), "11111111-1111-1111-1111-111111111111");
        ResultadoEntregaRequestDTO evt2 = new ResultadoEntregaRequestDTO(
                ResultadoEntregaRequestDTO.Resultado.ENTREGA_FALLIDA,
                "CLIENTE_AUSENTE", "Ausente", 6.2, -75.5, OffsetDateTime.now(), "22222222-2222-2222-2222-222222222222");

        SincronizacionEntregasDTO.Item item1 = new SincronizacionEntregasDTO.Item(100L, evt1);
        SincronizacionEntregasDTO.Item item2 = new SincronizacionEntregasDTO.Item(200L, evt2);
        SincronizacionEntregasDTO.Solicitud solicitud = new SincronizacionEntregasDTO.Solicitud(List.of(item1, item2));

        ResultadoEntregaResponseDTO resp1 = new ResultadoEntregaResponseDTO("11111111-1111-1111-1111-111111111111", 100L, ResultadoEntregaRequestDTO.Resultado.ENTREGADO, "APLICADO", OffsetDateTime.now(), false);
        when(registros.registrar(100L, evt1)).thenReturn(resp1);
        when(registros.registrar(200L, evt2)).thenThrow(new AccessDeniedException("No asignado al conductor"));

        SincronizacionEntregasDTO.Respuesta res = service.sincronizar(solicitud);

        assertThat(res.resultados()).hasSize(2);
        assertThat(res.resultados().get(0).idEventoCliente()).isEqualTo("11111111-1111-1111-1111-111111111111");
        assertThat(res.resultados().get(0).estado()).isEqualTo("APLICADO");
        assertThat(res.resultados().get(0).motivoRechazo()).isNull();

        assertThat(res.resultados().get(1).idEventoCliente()).isEqualTo("22222222-2222-2222-2222-222222222222");
        assertThat(res.resultados().get(1).estado()).isEqualTo("RECHAZADO");
        assertThat(res.resultados().get(1).motivoRechazo()).contains("No asignado al conductor");
    }
}
