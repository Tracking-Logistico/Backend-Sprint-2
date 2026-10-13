package com.udea.demo.pedidos.application.service;

import com.udea.demo.pedidos.application.dto.*;
import com.udea.demo.pedidos.domain.model.*;
import com.udea.demo.pedidos.interfaces.services.AccesoPedidoConductorI;
import com.udea.demo.usuarios.application.service.ActorAuthorizationService;
import com.udea.demo.usuarios.domain.model.Rol;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class AutorizacionLecturaPedido {
    private final ActorAuthorizationService actores;
    private final AccesoPedidoConductorI accesoConductor;

    public AutorizacionLecturaPedido(ActorAuthorizationService actores, AccesoPedidoConductorI accesoConductor) {
        this.actores = actores;
        this.accesoConductor = accesoConductor;
    }

    public void autorizarLectura(Pedido pedido) {
        var actor = actores.actorActual();
        if (actor.getRol() == Rol.CLIENTE && !pedido.getClienteId().equals(actores.clienteActualId()))
            throw new AccessDeniedException("El pedido no pertenece al cliente autenticado");
        if (actor.getRol() == Rol.CONDUCTOR && !accesoConductor.tieneAsignacionActiva(pedido.getId(), actor.getId()))
            throw new AccessDeniedException("El envío no está asignado al conductor autenticado");
    }

    public void autorizarConductor(Pedido pedido) {
        var actor = actores.actorActual();
        if (actor.getRol() == Rol.CONDUCTOR && !accesoConductor.fueAsignado(pedido.getId(), actor.getId()))
            throw new AccessDeniedException("El envío no está asignado al conductor autenticado");
    }

    public void autorizarHistorialConductor(Pedido pedido) {
        var actor = actores.actorActual();
        if (actor.getRol() != Rol.CONDUCTOR || !accesoConductor.fueAsignado(pedido.getId(), actor.getId()))
            throw new AccessDeniedException("El envío no está asignado al conductor autenticado");
    }
}
