package com.udea.demo.usuarios.infrastructure.rabbit;

import com.udea.demo.compartido.eventos.EventoIntegracion;
import com.udea.demo.usuarios.interfaces.services.NotificacionServiceI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConsumidorEventoNotificacion - consumidor RabbitMQ de eventos de logística")
class ConsumidorEventoNotificacionTest {

    @Mock
    private NotificacionServiceI notificacionService;

    @InjectMocks
    private ConsumidorEventoNotificacion consumidor;

    @Test
    @DisplayName("Delega el evento al servicio de notificaciones")
    void onEvento_delegaAServicio() {
        EventoIntegracion evento = new EventoIntegracion() {
            @Override public UUID eventId() { return UUID.randomUUID(); }
            @Override public java.time.LocalDateTime ocurridoEn() { return java.time.LocalDateTime.now(); }
            @Override public String tipo() { return "TEST"; }
            @Override public String routingKey() { return "pedido.test"; }
        };

        consumidor.onEvento(evento);

        verify(notificacionService).procesar(evento);
    }
}
