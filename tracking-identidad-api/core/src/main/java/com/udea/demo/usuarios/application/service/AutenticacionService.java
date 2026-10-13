package com.udea.demo.usuarios.application.service;


import org.springframework.stereotype.Service;

import com.udea.demo.usuarios.application.dto.LoginRequestDTO;
import com.udea.demo.usuarios.application.dto.LoginResponseDTO;
import com.udea.demo.usuarios.application.dto.RefreshTokenRequestDTO;
import com.udea.demo.usuarios.application.dto.RestablecerPasswordDTO;
import com.udea.demo.usuarios.application.dto.SolicitarRestablecimientoPasswordDTO;
import com.udea.demo.usuarios.interfaces.services.AutenticacionServiceI;

@Service
public class AutenticacionService implements AutenticacionServiceI {
    private final InicioSesionUseCase inicio;
    private final GestionSesionesUseCase sesiones;
    private final RestablecimientoCredencialesUseCase recuperacion;

    public AutenticacionService(InicioSesionUseCase inicio,
            GestionSesionesUseCase sesiones, RestablecimientoCredencialesUseCase recuperacion) {
        this.inicio = inicio;
        this.sesiones = sesiones;
        this.recuperacion = recuperacion;
    }

    @Override
    public LoginResponseDTO iniciarSesion(LoginRequestDTO request, String ip, String userAgent) {
        return inicio.iniciarSesion(request, ip, userAgent);
    }

    @Override
    public LoginResponseDTO renovarSesion(RefreshTokenRequestDTO request) {
        return sesiones.renovarSesion(request);
    }

    @Override
    public void cerrarSesion(String accessToken) {
        sesiones.cerrarSesion(accessToken);
    }

    @Override
    public void solicitarRestablecimiento(SolicitarRestablecimientoPasswordDTO request) {
        recuperacion.solicitarRestablecimiento(request);
    }

    @Override
    public void restablecerPassword(RestablecerPasswordDTO request) {
        recuperacion.restablecerPassword(request);
    }

    public static String hash(String value) {
        return TokensSeguros.hash(value);
    }
}
