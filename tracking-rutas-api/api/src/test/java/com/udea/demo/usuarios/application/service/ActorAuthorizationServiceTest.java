package com.udea.demo.usuarios.application.service;

import com.udea.demo.usuarios.domain.exception.PerfilRolIncompletoException;
import com.udea.demo.usuarios.domain.model.*;
import com.udea.demo.usuarios.interfaces.persistence.ClienteRepository;
import com.udea.demo.usuarios.interfaces.persistence.ConductorRepository;
import com.udea.demo.usuarios.interfaces.persistence.OperadorRepository;
import com.udea.demo.usuarios.interfaces.persistence.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ActorAuthorizationService - validación de roles y actores autenticados")
class ActorAuthorizationServiceTest {

    @Mock private UsuarioRepository usuarios;
    @Mock private OperadorRepository operadores;
    @Mock private ClienteRepository clientes;
    @Mock private ConductorRepository conductores;

    @InjectMocks
    private ActorAuthorizationService service;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarConEmail(String email) {
        var auth = new UsernamePasswordAuthenticationToken(email, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Lanza AccessDeniedException si no hay autenticación en el contexto")
    void actorActual_sinAutenticacion_lanzaAccessDenied() {
        assertThatThrownBy(() -> service.actorActual())
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Lanza AccessDeniedException si el usuario no existe en la base de datos o está inactivo")
    void actorActual_usuarioInactivo_lanzaAccessDenied() {
        autenticarConEmail("inactivo@test.com");
        Usuario usuario = Usuario.builder().email("inactivo@test.com").activo(false).build();
        when(usuarios.findByEmail("inactivo@test.com")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.actorActual())
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Exigir propietario pasa si el id y rol coinciden con el actor actual")
    void exigirPropietario_coincide_exitoso() {
        autenticarConEmail("user@test.com");
        Usuario usuario = Usuario.builder().id(10L).email("user@test.com").rol(Rol.CLIENTE).activo(true).build();
        when(usuarios.findByEmail("user@test.com")).thenReturn(Optional.of(usuario));

        service.exigirPropietario(10L, Rol.CLIENTE);
    }

    @Test
    @DisplayName("Exigir propietario lanza AccessDenied si el id no coincide")
    void exigirPropietario_idDistinto_lanzaAccessDenied() {
        autenticarConEmail("user@test.com");
        Usuario usuario = Usuario.builder().id(10L).email("user@test.com").rol(Rol.CLIENTE).activo(true).build();
        when(usuarios.findByEmail("user@test.com")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.exigirPropietario(20L, Rol.CLIENTE))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("clienteActualId retorna el id del cliente del usuario autenticado")
    void clienteActualId_exitoso() {
        autenticarConEmail("cliente@test.com");
        Usuario usuario = Usuario.builder().id(10L).email("cliente@test.com").rol(Rol.CLIENTE).activo(true).build();
        when(usuarios.findByEmail("cliente@test.com")).thenReturn(Optional.of(usuario));
        when(clientes.findByUsuarioId(10L)).thenReturn(Optional.of(Cliente.builder().id(50L).usuario(usuario).build()));

        Long id = service.clienteActualId();

        assertThat(id).isEqualTo(50L);
    }

    @Test
    @DisplayName("operadorActualId lanza PerfilRolIncompletoException si no tiene fila de operador")
    void operadorActualId_perfilIncompleto_lanzaExcepcion() {
        autenticarConEmail("operador@test.com");
        Usuario usuario = Usuario.builder().id(11L).email("operador@test.com").rol(Rol.OPERADOR).activo(true).build();
        when(usuarios.findByEmail("operador@test.com")).thenReturn(Optional.of(usuario));
        when(operadores.findByUsuarioId(11L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.operadorActualId())
                .isInstanceOf(PerfilRolIncompletoException.class);
    }

    @Test
    @DisplayName("conductorActualId y conductorActualUsuarioId retornan ids correspondientes")
    void conductorActual_exitoso() {
        autenticarConEmail("conductor@test.com");
        Usuario usuario = Usuario.builder().id(12L).email("conductor@test.com").rol(Rol.CONDUCTOR).activo(true).build();
        when(usuarios.findByEmail("conductor@test.com")).thenReturn(Optional.of(usuario));
        when(conductores.findByUsuarioId(12L)).thenReturn(Optional.of(Conductor.builder().id(80L).usuario(usuario).build()));

        assertThat(service.conductorActualUsuarioId()).isEqualTo(12L);
        assertThat(service.conductorActualId()).isEqualTo(80L);
    }
}
