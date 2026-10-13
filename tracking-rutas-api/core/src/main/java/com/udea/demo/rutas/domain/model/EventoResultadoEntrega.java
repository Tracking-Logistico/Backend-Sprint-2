package com.udea.demo.rutas.domain.model;
import com.udea.demo.rutas.application.dto.ResultadoEntregaRequestDTO;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "eventos_resultado_entrega")
public class EventoResultadoEntrega {
    @Id @Column(name = "id_evento_cliente", length = 36) private String idEventoCliente;
    @Column(name = "id_pedido", nullable = false) private Long pedidoId;
    @Column(name = "id_usuario", nullable = false) private Long usuarioId;
    @Enumerated(EnumType.STRING)
    @Column(name = "resultado", nullable = false, length = 40)
    private ResultadoEntregaRequestDTO.Resultado resultado;
    @Column(name = "codigo_novedad", length = 40) private String codigoNovedad;
    @Column(name = "motivo", length = 500) private String motivo;
    @Column(name = "fecha_evento", nullable = false) private OffsetDateTime fechaEvento;
    @Column(name = "fecha_registro", nullable = false) private OffsetDateTime fechaRegistro;

    protected EventoResultadoEntrega() {}
    public EventoResultadoEntrega(String idEventoCliente, Long pedidoId, Long usuarioId,
                                 ResultadoEntregaRequestDTO evento) {
        this.idEventoCliente = idEventoCliente;
        this.pedidoId = pedidoId;
        this.usuarioId = usuarioId;
        this.resultado = evento.resultado();
        this.codigoNovedad = evento.codigoNovedad();
        this.motivo = evento.motivo();
        this.fechaEvento = evento.fechaEvento();
        this.fechaRegistro = OffsetDateTime.now();
    }
    public String getIdEventoCliente() { return idEventoCliente; }
    public Long getPedidoId() { return pedidoId; }
    public Long getUsuarioId() { return usuarioId; }
    public ResultadoEntregaRequestDTO.Resultado getResultado() { return resultado; }
    public OffsetDateTime getFechaEvento() { return fechaEvento; }
    public String getCodigoNovedad() { return codigoNovedad; }
    public String getMotivo() { return motivo; }
}
