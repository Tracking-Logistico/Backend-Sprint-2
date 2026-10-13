package com.udea.demo.rutas.application.service;
import com.udea.demo.rutas.application.dto.SincronizacionEntregasDTO;
import com.udea.demo.usuarios.application.service.ActorAuthorizationService;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class SincronizacionEntregasService {
    private final RegistroResultadoEntregaService registros;
    private final ActorAuthorizationService actores;
    public SincronizacionEntregasService(RegistroResultadoEntregaService registros, ActorAuthorizationService actores) {
        this.registros = registros; this.actores = actores;
    }
    public SincronizacionEntregasDTO.Respuesta sincronizar(SincronizacionEntregasDTO.Solicitud solicitud) {
        actores.conductorActualUsuarioId();
        List<SincronizacionEntregasDTO.Resultado> resultados = new ArrayList<>();
        for (SincronizacionEntregasDTO.Item item : solicitud.eventos()) {
            String id = item != null && item.evento() != null ? item.evento().idEventoCliente() : null;
            try {
                var registrado = registros.registrar(item.pedidoId(), item.evento());
                resultados.add(new SincronizacionEntregasDTO.Resultado(registrado.idEventoCliente(), registrado.estado(), null));
            } catch (org.springframework.security.access.AccessDeniedException |
                     IllegalArgumentException | IllegalStateException |
                     com.udea.demo.pedidos.domain.exception.IncidenciaNoPermitidaException |
                     com.udea.demo.pedidos.domain.exception.TransicionEstadoInvalidaException |
                     com.udea.demo.pedidos.domain.exception.TipoIncidenciaInvalidoException |
                     com.udea.demo.pedidos.domain.exception.EnvioFinalizadoException |
                     com.udea.demo.pedidos.domain.exception.UbicacionInvalidaException ex) {
                resultados.add(new SincronizacionEntregasDTO.Resultado(id, "RECHAZADO", ex.getMessage()));
            }
        }
        return new SincronizacionEntregasDTO.Respuesta(resultados);
    }
}
