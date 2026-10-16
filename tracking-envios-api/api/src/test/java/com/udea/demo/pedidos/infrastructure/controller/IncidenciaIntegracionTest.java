package com.udea.demo.pedidos.infrastructure.controller;

import com.udea.demo.pedidos.domain.event.DevolucionIniciadaEvent;
import com.udea.demo.pedidos.domain.event.EntregaReprogramadaEvent;
import com.udea.demo.pedidos.domain.event.IncidenciaRegistradaEvent;
import com.udea.demo.pedidos.domain.event.VerificacionDireccionSolicitadaEvent;
import com.udea.demo.pedidos.domain.model.EstadoPedido;
import com.udea.demo.pedidos.domain.model.MotivoDevolucion;
import com.udea.demo.pedidos.domain.model.Pedido;
import com.udea.demo.pedidos.domain.model.Prioridad;
import com.udea.demo.pedidos.domain.model.TipoIncidencia;
import com.udea.demo.pedidos.domain.model.TipoServicio;
import com.udea.demo.pedidos.interfaces.persistence.IncidenciaPedidoRepository;
import com.udea.demo.pedidos.interfaces.persistence.PedidoRepository;
import com.udea.demo.rutas.domain.model.EstadoParada;
import com.udea.demo.rutas.domain.model.Ruta;
import com.udea.demo.rutas.interfaces.persistence.ParadaRutaRepository;
import com.udea.demo.rutas.interfaces.persistence.RutaRepository;
import com.udea.demo.usuarios.application.service.AutenticacionService;
import com.udea.demo.usuarios.domain.model.Cliente;
import com.udea.demo.usuarios.domain.model.Conductor;
import com.udea.demo.usuarios.domain.model.EstadoUsuario;
import com.udea.demo.usuarios.domain.model.Operador;
import com.udea.demo.usuarios.domain.model.Rol;
import com.udea.demo.usuarios.domain.model.SesionUsuario;
import com.udea.demo.usuarios.domain.model.Usuario;
import com.udea.demo.usuarios.interfaces.persistence.ClienteRepository;
import com.udea.demo.usuarios.interfaces.persistence.ConductorRepository;
import com.udea.demo.usuarios.interfaces.persistence.OperadorRepository;
import com.udea.demo.usuarios.interfaces.persistence.SesionUsuarioRepository;
import com.udea.demo.usuarios.interfaces.persistence.UsuarioRepository;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test de integración HTTP para incidencias HU-07. Sin @Transactional en nivel de clase
 * para que los datos del @BeforeEach sean visibles al SesionAuthenticationFilter
 * (que corre en su propia transacción JDBC separada). La limpieza es manual en @AfterEach.
 */
@SpringBootTest(properties = {"app.api.role=rutas", "app.messaging.rabbitmq.enabled=false", "spring.flyway.enabled=false"})
@ActiveProfiles("test")
@RecordApplicationEvents
@DisplayName("HU-07 Incidencias - integración HTTP, seguridad, seguimiento del cliente y rutas")
class IncidenciaIntegracionTest {
    @Autowired private WebApplicationContext context;
    @Autowired private Filter springSecurityFilterChain;
    private MockMvc mvc;
    @Autowired private ApplicationEvents eventos;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private SesionUsuarioRepository sesiones;
    @Autowired private OperadorRepository operadores;
    @Autowired private ClienteRepository clientes;
    @Autowired private ConductorRepository conductores;
    @Autowired private RutaRepository rutas;
    @Autowired private ParadaRutaRepository paradas;
    @Autowired private PedidoRepository pedidos;
    @Autowired private IncidenciaPedidoRepository incidencias;

    private String tokenOperador;
    private String tokenCliente;
    private Long clienteId;
    private Long conductorId;

    /** IDs de usuarios y pedidos creados en cada test para limpiar en @AfterEach. */
    private final List<Usuario> usuariosCreados = new ArrayList<>();
    private final List<Pedido> pedidosCreados = new ArrayList<>();

    @BeforeEach
    void datos() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(springSecurityFilterChain).build();
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        Usuario operador = usuario("operador-" + sufijo + "@tracking.com", Rol.OPERADOR);
        operadores.saveAndFlush(new Operador(null, operador, "OP-" + sufijo));
        tokenOperador = sesion(operador, "token-operador-" + sufijo);

        Usuario cliente = usuario("cliente-" + sufijo + "@tracking.com", Rol.CLIENTE);
        clienteId = clientes.saveAndFlush(new Cliente(null, cliente, "Medellín")).getId();
        tokenCliente = sesion(cliente, "token-cliente-" + sufijo);

        Usuario conductor = usuario("conductor-" + sufijo + "@tracking.com", Rol.CONDUCTOR);
        conductorId = conductores.saveAndFlush(Conductor.builder().usuario(conductor).licencia("LIC-" + sufijo)
                .estado("ACTIVO").build()).getId();
    }

    @AfterEach
    void limpiar() {
        for (Pedido p : pedidosCreados) {
            incidencias.deleteAll(incidencias.findByPedidoIdOrderByFechaAscIdAsc(p.getId()));
        }
        paradas.deleteAll();
        rutas.deleteAll();
        for (Pedido p : pedidosCreados) {
            pedidos.deleteById(p.getId());
        }
        pedidosCreados.clear();

        for (Usuario u : usuariosCreados) {
            sesiones.deleteAll(sesiones.findAll().stream()
                    .filter(s -> s.getUsuario().getId().equals(u.getId())).toList());
            operadores.deleteAll(operadores.findAll().stream()
                    .filter(o -> o.getUsuario().getId().equals(u.getId())).toList());
            clientes.deleteAll(clientes.findAll().stream()
                    .filter(c -> c.getUsuario().getId().equals(u.getId())).toList());
            conductores.deleteAll(conductores.findAll().stream()
                    .filter(c -> c.getUsuario().getId().equals(u.getId())).toList());
            usuarios.delete(u);
        }
        usuariosCreados.clear();
    }

    private Usuario usuario(String email, Rol rol) {
        Usuario u = usuarios.saveAndFlush(Usuario.builder().nombre("Usuario " + rol).email(email).password("hash").rol(rol)
                .estado(EstadoUsuario.ACTIVO).activo(true).aceptoTerminos(false).build());
        usuariosCreados.add(u);
        return u;
    }

    private String sesion(Usuario usuario, String token) {
        LocalDateTime ahora = LocalDateTime.now();
        sesiones.saveAndFlush(new SesionUsuario(usuario, AutenticacionService.hash(token),
                AutenticacionService.hash(token + "-refresh"), ahora.plusMinutes(15), ahora.plusDays(7), ahora));
        return token;
    }

    private Pedido envioEnReparto(String tracking) {
        String trackingUnico = tracking + "-" + UUID.randomUUID().toString().substring(0, 6);
        Pedido pedido = pedidos.saveAndFlush(Pedido.builder().numeroPedido("PED-" + trackingUnico).clienteId(clienteId)
                .direccionOrigen("Carrera 7 #71-21").direccionDestino("Calle 45 #12-30").ciudadDestino("Medellín")
                .descripcionPaquete("Caja").pesoKg(2.0).largoCm(10.0).anchoCm(10.0).altoCm(10.0)
                .tipoServicio(TipoServicio.ESTANDAR).prioridadSugerida(Prioridad.MEDIA).estado(EstadoPedido.EN_REPARTO)
                .fechaCreacion(LocalDateTime.now()).numeroTracking(trackingUnico).build());
        pedidosCreados.add(pedido);
        Ruta ruta = rutas.findByConductorIdAndFecha(conductorId, LocalDate.now())
                .orElseGet(() -> Ruta.crear(conductorId, LocalDate.now()));
        ruta.agregarParada(pedido.getId());
        rutas.saveAndFlush(ruta);
        return pedido;
    }

    private ResultActions incidencia(Long pedidoId, String json) throws Exception {
        return mvc.perform(post("/api/v1/pedidos/{id}/incidencias", pedidoId)
                .header("Authorization", "Bearer " + tokenOperador)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    @DisplayName("Cliente ausente → el cliente ve la novedad en su seguimiento y reprograma dentro del rango")
    void flujoEntregaFallidaYReprogramacion() throws Exception {
        Pedido pedido = envioEnReparto("LTHU07000001");

        incidencia(pedido.getId(), "{\"tipo\":\"CLIENTE_AUSENTE\",\"latitud\":6.24,\"longitud\":-75.58,\"precisionMetros\":10}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoPedido").value("ENTREGA_FALLIDA"))
                .andExpect(jsonPath("$.intentosEntregaFallidos").value(1))
                .andExpect(jsonPath("$.incidencia.descripcionTipo").value("Cliente ausente"));
        assertThat(eventos.stream(IncidenciaRegistradaEvent.class)).hasSize(1);

        mvc.perform(get("/api/v1/pedidos/mios/tracking/{t}", pedido.getNumeroTracking())
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENTREGA_FALLIDA"))
                .andExpect(jsonPath("$.novedad.titulo").value("Cliente ausente"))
                .andExpect(jsonPath("$.novedad.mensaje").value(TipoIncidencia.CLIENTE_AUSENTE.mensajeCliente()))
                .andExpect(jsonPath("$.movimientos[?(@.tipo == 'NOVEDAD')]").exists());

        mvc.perform(get("/api/v1/pedidos/mios/tracking/{t}/reprogramacion/rango", pedido.getNumeroTracking())
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desde").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.hasta").value(LocalDate.now().plusDays(7).toString()));

        mvc.perform(post("/api/v1/pedidos/mios/tracking/{t}/reprogramacion", pedido.getNumeroTracking())
                        .header("Authorization", "Bearer " + tokenCliente).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\":\"" + LocalDate.now().minusDays(1) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FECHA_REPROGRAMACION_INVALIDA"));

        mvc.perform(post("/api/v1/pedidos/mios/tracking/{t}/reprogramacion", pedido.getNumeroTracking())
                        .header("Authorization", "Bearer " + tokenCliente).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\":\"" + LocalDate.now().plusDays(2) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENTREGA_REPROGRAMADA"))
                .andExpect(jsonPath("$.fechaEntregaReprogramada").value(LocalDate.now().plusDays(2).toString()));
        assertThat(eventos.stream(EntregaReprogramadaEvent.class)).hasSize(1);
    }

    @Test
    @DisplayName("Validaciones: tipo inválido 400, 'Otro' sin comentario 400, operador requerido")
    void validaciones() throws Exception {
        Pedido pedido = envioEnReparto("LTHU07000002");

        incidencia(pedido.getId(), "{\"tipo\":\"PERDIDO\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INCIDENCIA_INVALIDA"));
        incidencia(pedido.getId(), "{\"tipo\":\"OTRO\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMENTARIO_OBLIGATORIO"));
        incidencia(pedido.getId(), "{\"tipo\":\"OTRO\",\"comentario\":\"Portería cerrada por evento\"}")
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/pedidos/{id}/incidencias", pedido.getId())
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"tipo\":\"OTRO\",\"comentario\":\"x\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/v1/pedidos/incidencias/tipos").header("Authorization", "Bearer " + tokenOperador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(TipoIncidencia.values().length));
        mvc.perform(get("/api/v1/pedidos/{id}/incidencias", pedido.getId()).header("Authorization", "Bearer " + tokenOperador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidencias.length()").value(1));
    }

    @Test
    @DisplayName("Dirección incorrecta → Dirección por verificar con plazo; el cliente confirma y vuelve a reparto")
    void direccionPorVerificar() throws Exception {
        Pedido pedido = envioEnReparto("LTHU07000003");

        incidencia(pedido.getId(), "{\"tipo\":\"DIRECCION_INCORRECTA\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoPedido").value("DIRECCION_POR_VERIFICAR"));
        assertThat(eventos.stream(VerificacionDireccionSolicitadaEvent.class)).hasSize(1);

        mvc.perform(put("/api/v1/pedidos/mios/tracking/{t}/direccion", pedido.getNumeroTracking())
                        .header("Authorization", "Bearer " + tokenCliente).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"direccionDestino\":\"Carrera 80 #45-12 Apto 301\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_REPARTO"));
        assertThat(pedidos.findById(pedido.getId()).orElseThrow().getDireccionDestino())
                .isEqualTo("Carrera 80 #45-12 Apto 301");
    }

    @Test
    @DisplayName("Paquete rechazado → Devolución al remitente, evento y cierre de la parada del conductor")
    void paqueteRechazado() throws Exception {
        Pedido pedido = envioEnReparto("LTHU07000004");

        incidencia(pedido.getId(), "{\"tipo\":\"PAQUETE_RECHAZADO\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoPedido").value("DEVOLUCION_AL_REMITENTE"));

        assertThat(eventos.stream(DevolucionIniciadaEvent.class))
                .singleElement().extracting(DevolucionIniciadaEvent::motivo).isEqualTo(MotivoDevolucion.PAQUETE_RECHAZADO);
        assertThat(paradas.findByPedidoIdAndEstado(pedido.getId(), EstadoParada.PENDIENTE)).isEmpty();

        incidencia(pedido.getId(), "{\"tipo\":\"RETRASO_OPERATIVO\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ENVIO_FINALIZADO"));
    }

    @Test
    @DisplayName("Tres intentos fallidos devuelven el envío y se rechaza un cuarto intento")
    void tresIntentos() throws Exception {
        Pedido pedido = envioEnReparto("LTHU07000005");
        for (int intento = 1; intento <= 2; intento++) {
            incidencia(pedido.getId(), "{\"tipo\":\"CLIENTE_AUSENTE\"}").andExpect(status().isCreated());
            mvc.perform(post("/api/v1/pedidos/mios/tracking/{t}/reprogramacion", pedido.getNumeroTracking())
                            .header("Authorization", "Bearer " + tokenCliente).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"fecha\":\"" + LocalDate.now().plusDays(1) + "\"}"))
                    .andExpect(status().isOk());
        }

        incidencia(pedido.getId(), "{\"tipo\":\"CLIENTE_AUSENTE\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoPedido").value("DEVOLUCION_AL_REMITENTE"))
                .andExpect(jsonPath("$.intentosEntregaFallidos").value(3));
        assertThat(eventos.stream(DevolucionIniciadaEvent.class))
                .singleElement().extracting(DevolucionIniciadaEvent::motivo).isEqualTo(MotivoDevolucion.MAXIMO_INTENTOS);

        incidencia(pedido.getId(), "{\"tipo\":\"CLIENTE_AUSENTE\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ENVIO_FINALIZADO"));
    }
}
