package com.udea.demo.rutas.application.service;

import com.udea.demo.rutas.domain.model.Ruta;
import com.udea.demo.rutas.interfaces.persistence.RutaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GestorRutaActiva - ruta del día operativo")
class GestorRutaActivaTest {
    private static final String ZONA = "America/Bogota";
    private static final Long CONDUCTOR_ID = 1L;

    @Mock private RutaRepository rutaRepository;
    private GestorRutaActiva gestor;

    @BeforeEach
    void zonaOperativa() {
        gestor = new GestorRutaActiva(rutaRepository);
        ReflectionTestUtils.setField(gestor, "zonaHoraria", ZONA);
    }

    private LocalDate hoy() {
        return LocalDate.now(ZoneId.of(ZONA));
    }

    @Test
    void retornaLaRutaExistenteDelDia() {
        Ruta existente = Ruta.crear(CONDUCTOR_ID, hoy());
        when(rutaRepository.findByConductorIdAndFecha(CONDUCTOR_ID, hoy())).thenReturn(Optional.of(existente));

        Ruta resultado = gestor.obtenerOCrear(CONDUCTOR_ID);

        assertThat(resultado).isSameAs(existente);
        verify(rutaRepository, never()).save(any());
    }

    @Test
    void creaYPersisteLaRutaSiNoExiste() {
        when(rutaRepository.findByConductorIdAndFecha(CONDUCTOR_ID, hoy())).thenReturn(Optional.empty());
        when(rutaRepository.save(any(Ruta.class))).thenAnswer(inv -> inv.getArgument(0));

        Ruta resultado = gestor.obtenerOCrear(CONDUCTOR_ID);

        assertThat(resultado.getConductorId()).isEqualTo(CONDUCTOR_ID);
        assertThat(resultado.getFecha()).isEqualTo(hoy());
        verify(rutaRepository).save(any(Ruta.class));
    }
}
