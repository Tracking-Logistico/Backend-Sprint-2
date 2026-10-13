package com.udea.demo.usuarios.application.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.udea.demo.usuarios.application.dto.RestablecerPasswordDTO;
import com.udea.demo.usuarios.application.dto.SolicitarRestablecimientoPasswordDTO;
import com.udea.demo.usuarios.domain.exception.PasswordNoCoincideException;
import com.udea.demo.usuarios.domain.exception.TokenRestablecimientoInvalidoException;
import com.udea.demo.usuarios.domain.model.EstadoUsuario;
import com.udea.demo.usuarios.domain.model.TokenRestablecimientoPassword;
import com.udea.demo.usuarios.domain.model.Usuario;
import com.udea.demo.usuarios.interfaces.persistence.IntentoInicioSesionRepository;
import com.udea.demo.usuarios.interfaces.persistence.SesionUsuarioRepository;
import com.udea.demo.usuarios.interfaces.persistence.TokenRestablecimientoPasswordRepository;
import com.udea.demo.usuarios.interfaces.persistence.UsuarioRepository;
import com.udea.demo.usuarios.interfaces.services.EmailServiceI;

@Service
public class RestablecimientoCredencialesUseCase {
    private final UsuarioRepository usuarioRepository;
    private final IntentoInicioSesionRepository intentoRepository;
    private final SesionUsuarioRepository sesionRepository;
    private final TokenRestablecimientoPasswordRepository resetRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailServiceI emailService;
    private final PasswordPolicyService passwordPolicyService;

    @Value("${app.auth.password-reset-minutes:45}")
    private long minutosReset;
    @Value("${app.auth.password-reset-url:http://localhost:8080/restablecer-password}")
    private String passwordResetUrl;

    public RestablecimientoCredencialesUseCase(UsuarioRepository usuarioRepository,
            IntentoInicioSesionRepository intentoRepository, SesionUsuarioRepository sesionRepository,
            TokenRestablecimientoPasswordRepository resetRepository, PasswordEncoder passwordEncoder,
            EmailServiceI emailService, PasswordPolicyService passwordPolicyService) {
        this.usuarioRepository = usuarioRepository;
        this.intentoRepository = intentoRepository;
        this.sesionRepository = sesionRepository;
        this.resetRepository = resetRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.passwordPolicyService = passwordPolicyService;
    }

    @Transactional
    public void solicitarRestablecimiento(SolicitarRestablecimientoPasswordDTO request) {
        usuarioRepository.findByEmail(request.email()).ifPresent(usuario -> {
            resetRepository.deleteByUsuarioId(usuario.getId());
            String token = TokensSeguros.generarToken();
            resetRepository.save(new TokenRestablecimientoPassword(
                    TokensSeguros.hash(token), usuario, LocalDateTime.now().plusMinutes(minutosReset)));
                emailService.enviarEnlaceRestablecimiento(request.email(), passwordResetUrl + "/" + token);
        });
    }

    @Transactional
    public void restablecerPassword(RestablecerPasswordDTO request) {
        if (!request.nuevaPassword().equals(request.confirmarPassword())) {
            throw new PasswordNoCoincideException();
        }
        TokenRestablecimientoPassword token = resetRepository.findByTokenHash(TokensSeguros.hash(request.token()))
                .orElseThrow(TokenRestablecimientoInvalidoException::new);
        if (token.getUsadoEn() != null || token.getExpiraEn().isBefore(LocalDateTime.now())) {
            throw new TokenRestablecimientoInvalidoException();
        }

        Usuario usuario = token.getUsuario();
        passwordPolicyService.validar(request.nuevaPassword(), usuario.getEmail(), usuario.getNombre());
        if (!Boolean.TRUE.equals(usuario.getActivo())
                || usuario.getEstado() == EstadoUsuario.INACTIVO
                || usuario.getEstado() == EstadoUsuario.BLOQUEADO) {
            throw new TokenRestablecimientoInvalidoException();
        }
        usuario.setPassword(passwordEncoder.encode(request.nuevaPassword()));

        if (usuario.getEstado() == EstadoUsuario.PENDIENTE_ACTIVACION) {
            usuario.setEstado(EstadoUsuario.ACTIVO);
        }
        usuarioRepository.save(usuario);
        token.setUsadoEn(LocalDateTime.now());
        resetRepository.save(token);
        sesionRepository.deleteByUsuarioId(usuario.getId());
        intentoRepository.deleteByEmail(usuario.getEmail());
    }
}
