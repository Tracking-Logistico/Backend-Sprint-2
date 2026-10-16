package com.udea.demo.usuarios.application.service;

import com.udea.demo.usuarios.application.dto.LoginRequestDTO;
import com.udea.demo.usuarios.application.dto.LoginResponseDTO;
import com.udea.demo.usuarios.application.dto.RefreshTokenRequestDTO;
import com.udea.demo.usuarios.application.dto.RestablecerPasswordDTO;
import com.udea.demo.usuarios.application.dto.SolicitarRestablecimientoPasswordDTO;
import com.udea.demo.usuarios.domain.model.Rol;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("AutenticacionService - delegación a casos de uso con DTOs reales y tipados")
class AutenticacionServiceDelegacionTest {
    private final InicioSesionUseCase inicio = mock(InicioSesionUseCase.class);
    private final GestionSesionesUseCase sesiones = mock(GestionSesionesUseCase.class);
    private final RestablecimientoCredencialesUseCase recuperacion = mock(RestablecimientoCredencialesUseCase.class);
    private final AutenticacionService fachada = new AutenticacionService(inicio, sesiones, recuperacion);

    private LoginRequestDTO loginRequest() {
        return new LoginRequestDTO("julian.castano@tracking.com", "PasswordSegura123!");
    }

    private LoginResponseDTO loginResponse(Rol rol) {
        LocalDateTime accesoExpira = LocalDateTime.of(2026, 10, 16, 9, 0);
        LocalDateTime refreshExpira = LocalDateTime.of(2026, 10, 23, 8, 45);
        return new LoginResponseDTO(
                "access-token-sesion", "refresh-token-sesion",
                accesoExpira, refreshExpira, rol, "/panel/" + rol.name().toLowerCase(),
                7L, false);
    }

    @Test
    void delegaInicioDeSesion() {
        LoginRequestDTO solicitud = loginRequest();
        LoginResponseDTO respuesta = loginResponse(Rol.CLIENTE);
        String ip = "181.49.12.30";
        String userAgent = "Mozilla/5.0";
        when(inicio.iniciarSesion(solicitud, ip, userAgent)).thenReturn(respuesta);

        assertSame(respuesta, fachada.iniciarSesion(solicitud, ip, userAgent));
        verify(inicio).iniciarSesion(solicitud, ip, userAgent);
        verifyNoInteractions(sesiones, recuperacion);
    }

    @Test
    void delegaRenovacionDeSesion() {
        RefreshTokenRequestDTO solicitud = new RefreshTokenRequestDTO("refresh-token-sesion");
        LoginResponseDTO respuesta = loginResponse(Rol.OPERADOR);
        when(sesiones.renovarSesion(solicitud)).thenReturn(respuesta);

        assertSame(respuesta, fachada.renovarSesion(solicitud));
        verify(sesiones).renovarSesion(solicitud);
        verifyNoInteractions(inicio, recuperacion);
    }

    @Test
    void delegaCierreDeSesion() {
        String accessToken = "access-token-sesion";

        fachada.cerrarSesion(accessToken);

        verify(sesiones).cerrarSesion(accessToken);
        verifyNoInteractions(inicio, recuperacion);
    }

    @Test
    void delegaSolicitudDeRestablecimiento() {
        SolicitarRestablecimientoPasswordDTO solicitud =
                new SolicitarRestablecimientoPasswordDTO("julian.castano@tracking.com");

        fachada.solicitarRestablecimiento(solicitud);

        verify(recuperacion).solicitarRestablecimiento(solicitud);
        verifyNoInteractions(inicio, sesiones);
    }

    @Test
    void delegaRestablecimientoDePassword() {
        RestablecerPasswordDTO solicitud = new RestablecerPasswordDTO(
                "token-restablecimiento", "NuevaClave123!", "NuevaClave123!");

        fachada.restablecerPassword(solicitud);

        verify(recuperacion).restablecerPassword(solicitud);
        verifyNoInteractions(inicio, sesiones);
    }
}
