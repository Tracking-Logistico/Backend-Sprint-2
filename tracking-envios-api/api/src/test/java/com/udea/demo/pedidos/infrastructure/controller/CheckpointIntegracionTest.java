package com.udea.demo.pedidos.infrastructure.controller;

import com.udea.demo.pedidos.domain.event.CheckpointPendienteRevisionEvent;
import com.udea.demo.pedidos.domain.event.CheckpointRegistradoEvent;
import com.udea.demo.pedidos.domain.model.*;
import com.udea.demo.pedidos.interfaces.persistence.CheckpointPedidoRepository;
import com.udea.demo.pedidos.interfaces.persistence.HistorialPedidoRepository;
import com.udea.demo.pedidos.interfaces.persistence.PedidoRepository;
import com.udea.demo.rutas.domain.model.Ruta;
import com.udea.demo.rutas.interfaces.persistence.RutaRepository;
import com.udea.demo.usuarios.application.service.AutenticacionService;
import com.udea.demo.usuarios.domain.model.*;
import com.udea.demo.usuarios.interfaces.persistence.ConductorRepository;
import com.udea.demo.usuarios.interfaces.persistence.OperadorRepository;
import com.udea.demo.usuarios.interfaces.persistence.SesionUsuarioRepository;
import com.udea.demo.usuarios.interfaces.persistence.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test de integración HTTP para checkpoints. Sin @Transactional en el nivel de clase
 * para que los datos del @BeforeEach sean visibles al SesionAuthenticationFilter
 * (que corre en su propia transacción JDBC separada).
 */
@SpringBootTest(properties = {"app.api.role=rutas", "app.messaging.rabbitmq.enabled=false", "spring.flyway.enabled=false"})
@org.springframework.test.context.ActiveProfiles("test")
@RecordApplicationEvents
@DisplayName("HU-06 Checkpoints - integración HTTP, seguridad y persistencia")
class CheckpointIntegracionTest {
    @Autowired private WebApplicationContext context;
    @Autowired private jakarta.servlet.Filter springSecurityFilterChain;
    private MockMvc mvc;
    @Autowired private ApplicationEvents eventos;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private SesionUsuarioRepository sesiones;
    @Autowired private ConductorRepository conductores;
    @Autowired private OperadorRepository operadores;
    @Autowired private RutaRepository rutas;
    @Autowired private PedidoRepository pedidos;
    @Autowired private CheckpointPedidoRepository checkpoints;
    @Autowired private HistorialPedidoRepository historial;

    private Pedido pedido;
    private String tokenConductor;
    private Usuario conductorUsuario;
    private Conductor conductorPerfil;
    private Ruta ruta;

    @BeforeEach
    void datos() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(springSecurityFilterChain).build();
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        conductorUsuario = usuario("conductor-" + sufijo + "@tracking.com", Rol.CONDUCTOR);
        tokenConductor = sesion(conductorUsuario, "token-conductor-" + sufijo);
        conductorPerfil = conductores.saveAndFlush(Conductor.builder().usuario(conductorUsuario).licencia("LIC-" + sufijo)
                .estado("ACTIVO").build());
        pedido = pedidos.saveAndFlush(Pedido.builder().numeroPedido("PED-" + sufijo).clienteId(10L)
                .direccionOrigen("Carrera 7 #71-21").direccionDestino("Calle 45 #12-30").descripcionPaquete("Caja")
                .pesoKg(2.0).largoCm(10.0).anchoCm(10.0).altoCm(10.0).tipoServicio(TipoServicio.ESTANDAR)
                .prioridadSugerida(Prioridad.MEDIA).estado(EstadoPedido.EN_REPARTO).fechaCreacion(LocalDateTime.now())
                .numeroTracking("LT" + sufijo).build());
        ruta = Ruta.crear(conductorPerfil.getId(), LocalDate.now());
        ruta.agregarParada(pedido.getId());
        ruta = rutas.saveAndFlush(ruta);
    }

    @AfterEach
    void limpiar() {
        checkpoints.deleteAll(checkpoints.findAll().stream()
                .filter(c -> c.getPedidoId().equals(pedido.getId())).toList());
        rutas.delete(ruta);
        pedidos.deleteById(pedido.getId());
        sesiones.deleteAll(sesiones.findAll().stream()
                .filter(s -> s.getUsuario().getId().equals(conductorUsuario.getId())).toList());
        conductores.delete(conductorPerfil);
        // Limpiamos usuarios extra creados durante tests (operador, otro conductor)
        usuarios.findAll().stream()
                .filter(u -> u.getEmail().endsWith("@tracking.com")
                        && !u.getId().equals(conductorUsuario.getId()))
                .forEach(u -> {
                    sesiones.deleteAll(sesiones.findAll().stream()
                            .filter(s -> s.getUsuario().getId().equals(u.getId())).toList());
                    operadores.findAll().stream()
                            .filter(o -> o.getUsuario().getId().equals(u.getId()))
                            .forEach(operadores::delete);
                    conductores.findAll().stream()
                            .filter(c -> c.getUsuario().getId().equals(u.getId()))
                            .forEach(conductores::delete);
                    usuarios.delete(u);
                });
        usuarios.delete(conductorUsuario);
    }

    private Usuario usuario(String email, Rol rol) {
        return usuarios.saveAndFlush(Usuario.builder().nombre("Usuario " + rol).email(email).password("hash").rol(rol)
                .estado(EstadoUsuario.ACTIVO).activo(true).aceptoTerminos(false).build());
    }

    private String sesion(Usuario usuario, String token) {
        LocalDateTime ahora = LocalDateTime.now();
        sesiones.saveAndFlush(new SesionUsuario(usuario, AutenticacionService.hash(token),
                AutenticacionService.hash(token + "-refresh"), ahora.plusMinutes(15), ahora.plusDays(7), ahora));
        return token;
    }

    private String checkpointJson(String idEvento, String qr) {
        return """
                {"codigoQr":"%s","etapa":"EN_REPARTO","idEventoCliente":"%s",
                 "latitud":6.2442,"longitud":-75.5812,"precisionMetros":8,"fechaDispositivo":"%s"}
                """.formatted(qr, idEvento, OffsetDateTime.now().minusMinutes(1));
    }

    private String qrValido() { return CodigoQrEnvio.para(pedido.getNumeroTracking()).contenido(); }

    @Test
    @DisplayName("El conductor asignado registra el checkpoint: 201, línea de tiempo y evento publicado")
    void registraCheckpoint() throws Exception {
        String idEvento = UUID.randomUUID().toString();

        mvc.perform(post("/api/v1/pedidos/{id}/checkpoints", pedido.getId())
                        .header("Authorization", "Bearer " + tokenConductor)
                        .contentType(MediaType.APPLICATION_JSON).content(checkpointJson(idEvento, qrValido())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoRegistro").value("APLICADO"))
                .andExpect(jsonPath("$.ubicacionConfiable").value(true));

        assertThat(checkpoints.findByIdEventoCliente(idEvento)).isPresent();
        assertThat(historial.findByPedidoIdOrderByFechaAsc(pedido.getId()))
                .anyMatch(h -> "CHECKPOINT".equals(h.getTipoEvento()));
        assertThat(eventos.stream(CheckpointRegistradoEvent.class)).hasSize(1);

        // Idempotencia: segundo envío con mismo idEvento devuelve 200
        mvc.perform(post("/api/v1/pedidos/{id}/checkpoints", pedido.getId())
                        .header("Authorization", "Bearer " + tokenConductor)
                        .contentType(MediaType.APPLICATION_JSON).content(checkpointJson(idEvento, qrValido())))
                .andExpect(status().isOk());
        assertThat(checkpoints.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("QR alterado responde 400 QR_INVALIDO")
    void qrInvalido() throws Exception {
        mvc.perform(post("/api/v1/pedidos/{id}/checkpoints", pedido.getId())
                        .header("Authorization", "Bearer " + tokenConductor).contentType(MediaType.APPLICATION_JSON)
                        .content(checkpointJson(UUID.randomUUID().toString(), pedido.getNumeroTracking() + "|ZZZZZZZZ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("QR_INVALIDO"));
    }

    @Test
    @DisplayName("Un conductor sin asignación recibe 403 ENVIO_NO_ASIGNADO")
    void conductorNoAsignado() throws Exception {
        String s = UUID.randomUUID().toString().substring(0, 8);
        Usuario otroConductorUsuario = usuario("otro-conductor-" + s + "@tracking.com", Rol.CONDUCTOR);
        String otroToken = sesion(otroConductorUsuario, "token-otro-" + s);

        mvc.perform(post("/api/v1/pedidos/{id}/checkpoints", pedido.getId())
                        .header("Authorization", "Bearer " + otroToken).contentType(MediaType.APPLICATION_JSON)
                        .content(checkpointJson(UUID.randomUUID().toString(), qrValido())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ENVIO_NO_ASIGNADO"));
    }

    @Test
    @DisplayName("Un cliente no puede registrar checkpoints")
    void clienteNoAutorizado() throws Exception {
        String s = UUID.randomUUID().toString().substring(0, 8);
        Usuario clienteUsuario = usuario("cliente-" + s + "@tracking.com", Rol.CLIENTE);
        String tokenCliente = sesion(clienteUsuario, "token-cliente-" + s);

        mvc.perform(post("/api/v1/pedidos/{id}/checkpoints", pedido.getId())
                        .header("Authorization", "Bearer " + tokenCliente).contentType(MediaType.APPLICATION_JSON)
                        .content(checkpointJson(UUID.randomUUID().toString(), qrValido())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Envío entregado: en línea 409 ENVIO_FINALIZADO; sincronizado queda pendiente de revisión")
    void envioEntregado() throws Exception {
        Pedido entregado = pedidos.findById(pedido.getId()).orElseThrow();
        entregado.cambiarEstadoLogistico(EstadoPedido.ENTREGADO);
        pedidos.saveAndFlush(entregado);

        mvc.perform(post("/api/v1/pedidos/{id}/checkpoints", pedido.getId())
                        .header("Authorization", "Bearer " + tokenConductor).contentType(MediaType.APPLICATION_JSON)
                        .content(checkpointJson(UUID.randomUUID().toString(), qrValido())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ENVIO_FINALIZADO"));

        String lote = """
                {"eventos":[{"pedidoId":%d,"checkpoint":%s}]}
                """.formatted(pedido.getId(), checkpointJson(UUID.randomUUID().toString(), qrValido()));
        mvc.perform(post("/api/v1/pedidos/checkpoints/sincronizacion")
                        .header("Authorization", "Bearer " + tokenConductor)
                        .contentType(MediaType.APPLICATION_JSON).content(lote))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendientesRevision").value(1))
                .andExpect(jsonPath("$.aplicados").value(0));
        assertThat(eventos.stream(CheckpointPendienteRevisionEvent.class)).hasSize(1);

        mvc.perform(get("/api/v1/pedidos/checkpoints/mios/pendientes-revision")
                        .header("Authorization", "Bearer " + tokenConductor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    @DisplayName("El operador ve el checkpoint en su panel de novedades")
    void novedadesOperador() throws Exception {
        mvc.perform(post("/api/v1/pedidos/{id}/checkpoints", pedido.getId())
                        .header("Authorization", "Bearer " + tokenConductor).contentType(MediaType.APPLICATION_JSON)
                        .content(checkpointJson(UUID.randomUUID().toString(), qrValido())))
                .andExpect(status().isCreated());
        String s = UUID.randomUUID().toString().substring(0, 8);
        Usuario operadorUsuario = usuario("operador-" + s + "@tracking.com", Rol.OPERADOR);
        String tokenOperador = sesion(operadorUsuario, "token-operador-" + s);
        Operador perfil = new Operador();
        perfil.setUsuario(operadorUsuario);
        perfil.setCodigoEmpleado("OP-" + s);
        operadores.saveAndFlush(perfil);

        mvc.perform(get("/api/v1/pedidos/novedades").header("Authorization", "Bearer " + tokenOperador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.pedidoId == %d && @.tipoEvento == 'CHECKPOINT')]",
                        pedido.getId()).exists());
    }
}
