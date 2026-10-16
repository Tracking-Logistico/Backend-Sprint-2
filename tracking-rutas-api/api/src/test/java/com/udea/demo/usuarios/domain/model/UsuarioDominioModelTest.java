package com.udea.demo.usuarios.domain.model;

import com.udea.demo.usuarios.domain.exception.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Modelos y excepciones del dominio de Usuarios")
class UsuarioDominioModelTest {

    @Test
    @DisplayName("Usuario - creación con builder, setters y propiedades")
    void testUsuarioModel() {
        LocalDateTime ahora = LocalDateTime.now();
        Usuario usuario = Usuario.builder()
                .id(1L)
                .nombre("Juan")
                .email("juan@test.com")
                .password("passHash")
                .telefono("3001234567")
                .direccion("Calle 1")
                .rol(Rol.CLIENTE)
                .estado(EstadoUsuario.PENDIENTE_VERIFICACION)
                .activo(true)
                .aceptoTerminos(true)
                .versionTerminos("1.0")
                .fechaAceptacionTerminos(ahora)
                .aceptoPoliticaDatos(true)
                .versionPoliticaDatos("1.0")
                .fechaAceptacionPoliticaDatos(ahora)
                .fechaCreacion(ahora)
                .build();

        assertThat(usuario.getId()).isEqualTo(1L);
        assertThat(usuario.getNombre()).isEqualTo("Juan");
        assertThat(usuario.getEmail()).isEqualTo("juan@test.com");
        assertThat(usuario.getRol()).isEqualTo(Rol.CLIENTE);
        assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.PENDIENTE_VERIFICACION);
        assertThat(usuario.getActivo()).isTrue();
        assertThat(usuario.getVersionTerminos()).isEqualTo("1.0");

        usuario.setNombre("Juan Modificado");
        usuario.setTelefono("3119998877");
        usuario.setDireccion("Nueva Calle");
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario.setActivo(false);

        assertThat(usuario.getNombre()).isEqualTo("Juan Modificado");
        assertThat(usuario.getTelefono()).isEqualTo("3119998877");
        assertThat(usuario.getDireccion()).isEqualTo("Nueva Calle");
        assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(usuario.getActivo()).isFalse();
    }

    @Test
    @DisplayName("Cliente, Conductor y Operador - modelos y builders")
    void testRolesModel() {
        Usuario u = Usuario.builder().id(10L).nombre("Test").build();

        Cliente cliente = Cliente.builder().id(1L).usuario(u).ciudad("Medellin").build();
        assertThat(cliente.getId()).isEqualTo(1L);
        assertThat(cliente.getCiudad()).isEqualTo("Medellin");
        assertThat(cliente.getUsuario()).isEqualTo(u);
        cliente.setCiudad("Envigado");
        assertThat(cliente.getCiudad()).isEqualTo("Envigado");

        Conductor conductor = Conductor.builder().id(2L).usuario(u).licencia("LIC-999").estado("ACTIVO").build();
        assertThat(conductor.getId()).isEqualTo(2L);
        assertThat(conductor.getLicencia()).isEqualTo("LIC-999");
        assertThat(conductor.getEstado()).isEqualTo("ACTIVO");
        conductor.setLicencia("LIC-888");
        conductor.setEstado("INACTIVO");
        assertThat(conductor.getLicencia()).isEqualTo("LIC-888");
        assertThat(conductor.getEstado()).isEqualTo("INACTIVO");

        Operador operador = Operador.builder().id(3L).usuario(u).codigoEmpleado("EMP-77").build();
        assertThat(operador.getId()).isEqualTo(3L);
        assertThat(operador.getCodigoEmpleado()).isEqualTo("EMP-77");
        operador.setCodigoEmpleado("EMP-88");
        assertThat(operador.getCodigoEmpleado()).isEqualTo("EMP-88");
    }

    @Test
    @DisplayName("TokenVerificacion - expiración y builder")
    void testTokenVerificacion() {
        Usuario u = Usuario.builder().id(5L).build();
        TokenVerificacion tv = TokenVerificacion.builder()
                .id(1L)
                .token("hashToken")
                .usuario(u)
                .fechaExpiracion(LocalDateTime.now().plusHours(2))
                .build();

        assertThat(tv.getId()).isEqualTo(1L);
        assertThat(tv.getToken()).isEqualTo("hashToken");
        assertThat(tv.getUsuario()).isEqualTo(u);
        assertThat(tv.estaExpirado()).isFalse();

        tv.setFechaExpiracion(LocalDateTime.now().minusHours(1));
        assertThat(tv.estaExpirado()).isTrue();
    }

    @Test
    @DisplayName("TokenRestablecimientoPassword - modelo y expiración")
    void testTokenRestablecimiento() {
        Usuario u = Usuario.builder().id(8L).build();
        LocalDateTime expira = LocalDateTime.now().plusMinutes(45);
        TokenRestablecimientoPassword token = new TokenRestablecimientoPassword("hash123", u, expira);

        assertThat(token.getUsuario()).isEqualTo(u);
        assertThat(token.getExpiraEn()).isEqualTo(expira);
        assertThat(token.getUsadoEn()).isNull();

        LocalDateTime usado = LocalDateTime.now();
        token.setUsadoEn(usado);
        assertThat(token.getUsadoEn()).isEqualTo(usado);
    }

    @Test
    @DisplayName("SesionUsuario - modelo y vigencia")
    void testSesionUsuario() {
        Usuario u = Usuario.builder().id(12L).build();
        LocalDateTime ahora = LocalDateTime.now();
        SesionUsuario sesion = new SesionUsuario(
                u, "accHash", "refHash", ahora.plusHours(1), ahora.plusDays(7), ahora);

        assertThat(sesion.getUsuario()).isEqualTo(u);
        assertThat(sesion.getAccessTokenHash()).isEqualTo("accHash");
        assertThat(sesion.getRefreshTokenHash()).isEqualTo("refHash");
        assertThat(sesion.getRevokedAt()).isNull();

        sesion.setRevokedAt(ahora);
        assertThat(sesion.getRevokedAt()).isEqualTo(ahora);
        sesion.setLastActivityAt(ahora.plusMinutes(5));
        assertThat(sesion.getLastActivityAt()).isEqualTo(ahora.plusMinutes(5));
    }

    @Test
    @DisplayName("NotificacionEnviada - transiciones y registro de fallos")
    void testNotificacionEnviada() {
        NotificacionEnviada notif = NotificacionEnviada.pendiente(
                "evt-1", "CHECKPOINT", 50L, 100L, "LT-123",
                CanalNotificacion.EMAIL, "test@test.com", "Asunto", "Cuerpo");

        assertThat(notif.getEventId()).isEqualTo("evt-1");
        assertThat(notif.getEstado()).isEqualTo(EstadoNotificacion.PENDIENTE);
        assertThat(notif.getIntentos()).isZero();

        notif.marcarEnviada();
        assertThat(notif.getEstado()).isEqualTo(EstadoNotificacion.ENVIADA);
        assertThat(notif.getFechaUltimoIntento()).isNotNull();

        notif.registrarFallo("Timeout");
        assertThat(notif.getEstado()).isEqualTo(EstadoNotificacion.FALLIDA);
        assertThat(notif.getIntentos()).isEqualTo(2);
        assertThat(notif.getMotivoFallo()).isEqualTo("Timeout");
    }

    @Test
    @DisplayName("Excepciones del dominio de usuarios")
    void testExcepciones() {
        assertThatThrownBy(() -> { throw new CuentaBloqueadaLoginException(LocalDateTime.now()); })
                .isInstanceOf(CuentaBloqueadaLoginException.class)
                .hasMessageContaining("bloqueada");

        assertThatThrownBy(() -> { throw new PerfilRolIncompletoException(Rol.OPERADOR, 1L); })
                .isInstanceOf(PerfilRolIncompletoException.class);

        assertThatThrownBy(() -> { throw new UsuarioNoEncontradoException(99L); })
                .isInstanceOf(UsuarioNoEncontradoException.class);

        assertThatThrownBy(() -> { throw new EntregaCorreoException(); })
                .isInstanceOf(EntregaCorreoException.class);

        assertThatThrownBy(() -> { throw new TokenRestablecimientoInvalidoException(); })
                .isInstanceOf(TokenRestablecimientoInvalidoException.class);

        assertThatThrownBy(() -> { throw new SesionInvalidaException(); })
                .isInstanceOf(SesionInvalidaException.class);

        assertThatThrownBy(() -> { throw new EmailYaRegistradoException("dup@test.com"); })
                .isInstanceOf(EmailYaRegistradoException.class);

        assertThatThrownBy(() -> { throw new RolInternoInvalidoException(); })
                .isInstanceOf(RolInternoInvalidoException.class);

        assertThatThrownBy(() -> { throw new CredencialesInvalidasException(); })
                .isInstanceOf(CredencialesInvalidasException.class);

        assertThatThrownBy(() -> { throw new CuentaInactivaException(); })
                .isInstanceOf(CuentaInactivaException.class);

        assertThatThrownBy(() -> { throw new LicenciaRequeridaException(); })
                .isInstanceOf(LicenciaRequeridaException.class);

        assertThatThrownBy(() -> { throw new PasswordDebilException("Debil"); })
                .isInstanceOf(PasswordDebilException.class);

        assertThatThrownBy(() -> { throw new PasswordNoCoincideException(); })
                .isInstanceOf(PasswordNoCoincideException.class);

        assertThatThrownBy(() -> { throw new CodigoEmpleadoRequeridoException(); })
                .isInstanceOf(CodigoEmpleadoRequeridoException.class);
    }
}
