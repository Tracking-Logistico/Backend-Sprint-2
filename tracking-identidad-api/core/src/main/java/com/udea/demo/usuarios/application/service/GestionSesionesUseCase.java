package com.udea.demo.usuarios.application.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.udea.demo.usuarios.application.dto.LoginResponseDTO;
import com.udea.demo.usuarios.application.dto.RefreshTokenRequestDTO;
import com.udea.demo.usuarios.domain.exception.SesionInvalidaException;
import com.udea.demo.usuarios.domain.model.EstadoUsuario;
import com.udea.demo.usuarios.domain.model.Rol;
import com.udea.demo.usuarios.domain.model.SesionUsuario;
import com.udea.demo.usuarios.domain.model.Usuario;
import com.udea.demo.usuarios.interfaces.persistence.SesionUsuarioRepository;

@Service
public class GestionSesionesUseCase {
    private final SesionUsuarioRepository sesionRepository;
    @Value("${app.auth.refresh-token-days:7}")
    private long diasRefresh;
    @Value("${app.auth.client-inactivity-minutes:30}")
    private long minutosInactividadCliente;
    @Value("${app.auth.operational-inactivity-minutes:15}")
    private long minutosInactividadOperativo;

    public GestionSesionesUseCase(SesionUsuarioRepository sesionRepository) {
        this.sesionRepository = sesionRepository;
    }

    @Transactional
    public LoginResponseDTO renovarSesion(RefreshTokenRequestDTO request) {
        LocalDateTime ahora = LocalDateTime.now();
        SesionUsuario sesion = sesionRepository.findByRefreshTokenHash(TokensSeguros.hash(request.refreshToken()))
                .orElseThrow(SesionInvalidaException::new);
        long minutosInactividad = sesion.getUsuario().getRol() == Rol.CLIENTE
                ? minutosInactividadCliente : minutosInactividadOperativo;
        if (sesion.getRevokedAt() != null || sesion.getRefreshTokenExpiresAt().isBefore(ahora)
                || !sesion.getLastActivityAt().plusMinutes(minutosInactividad).isAfter(ahora)
                || !Boolean.TRUE.equals(sesion.getUsuario().getActivo())
                || !(sesion.getUsuario().getEstado() == EstadoUsuario.ACTIVO
                    || sesion.getUsuario().getEstado() == EstadoUsuario.PENDIENTE_ACTIVACION
                    || (sesion.getUsuario().getRol() == Rol.CLIENTE
                        && sesion.getUsuario().getEstado() == EstadoUsuario.PENDIENTE_VERIFICACION))) {
            throw new SesionInvalidaException();
        }
        sesion.setRevokedAt(ahora);
        return crearSesion(sesion.getUsuario(), ahora);
    }

    @Transactional
    public void cerrarSesion(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return;
        }
        sesionRepository.findByAccessTokenHash(TokensSeguros.hash(accessToken)).ifPresent(s -> {
            s.setRevokedAt(LocalDateTime.now());
            sesionRepository.save(s);
        });
    }

    public LoginResponseDTO crearSesion(Usuario usuario, LocalDateTime ahora) {
        String accessToken = TokensSeguros.generarToken();
        String refreshToken = TokensSeguros.generarToken();
        long minutosInactividad = usuario.getRol() == Rol.CLIENTE
            ? minutosInactividadCliente : minutosInactividadOperativo;
        LocalDateTime accessExpira = ahora.plusMinutes(minutosInactividad);
        LocalDateTime refreshExpira = ahora.plusDays(diasRefresh);
        sesionRepository.save(new SesionUsuario(usuario, TokensSeguros.hash(accessToken), TokensSeguros.hash(refreshToken),
                accessExpira, refreshExpira, ahora));
        boolean requiereCambio = usuario.getEstado() == EstadoUsuario.PENDIENTE_ACTIVACION;
        return new LoginResponseDTO(accessToken, refreshToken, accessExpira, refreshExpira,
                usuario.getRol(), requiereCambio ? "/panel/configuracion" : panelDe(usuario.getRol()),
                usuario.getId(), requiereCambio);
    }

    private String panelDe(Rol rol) {
        return switch (rol) {
            case CLIENTE -> "/panel/cliente";
            case OPERADOR -> "/panel/operador";
            case CONDUCTOR -> "/panel/conductor";
        };
    }
}
