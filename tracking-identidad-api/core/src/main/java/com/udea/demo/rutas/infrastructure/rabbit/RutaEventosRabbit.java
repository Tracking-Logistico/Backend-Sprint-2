package com.udea.demo.rutas.infrastructure.rabbit;

import com.udea.demo.pedidos.domain.event.CheckpointRegistradoEvent;
import com.udea.demo.pedidos.domain.event.DevolucionIniciadaEvent;
import com.udea.demo.rutas.application.service.CierreParadaPorDevolucionListener;
import com.udea.demo.rutas.application.service.MarcarParadaEntregadaListener;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@ConditionalOnProperty(name = "app.api.role", havingValue = "rutas")
@ConditionalOnProperty(name = "app.messaging.rabbitmq.enabled", havingValue = "true")
class RutaEventosRabbitConfig {
    static final String COLA = "rutas.operaciones.queue";

    @Bean
    Queue rutaEventosQueue() {
        return QueueBuilder.durable(COLA).build();
    }

    @Bean
    Binding rutaCheckpointBinding(Queue rutaEventosQueue, TopicExchange eventosLogisticaExchange) {
        return BindingBuilder.bind(rutaEventosQueue).to(eventosLogisticaExchange)
                .with(CheckpointRegistradoEvent.ROUTING_KEY);
    }

    @Bean
    Binding rutaDevolucionBinding(Queue rutaEventosQueue, TopicExchange eventosLogisticaExchange) {
        return BindingBuilder.bind(rutaEventosQueue).to(eventosLogisticaExchange)
                .with(DevolucionIniciadaEvent.ROUTING_KEY);
    }
}

@Component
@ConditionalOnProperty(name = "app.api.role", havingValue = "rutas")
@ConditionalOnProperty(name = "app.messaging.rabbitmq.enabled", havingValue = "true")
class RutaEventosRabbitConsumer {
    private final MarcarParadaEntregadaListener checkpoint;
    private final CierreParadaPorDevolucionListener devolucion;

    RutaEventosRabbitConsumer(MarcarParadaEntregadaListener checkpoint,
                              CierreParadaPorDevolucionListener devolucion) {
        this.checkpoint = checkpoint;
        this.devolucion = devolucion;
    }

    @RabbitListener(queues = RutaEventosRabbitConfig.COLA)
    @Transactional
    public void recibir(com.udea.demo.compartido.eventos.EventoIntegracion evento) {
        if (evento instanceof CheckpointRegistradoEvent recibido) {
            checkpoint.alRegistrarCheckpoint(recibido);
        } else if (evento instanceof DevolucionIniciadaEvent recibido) {
            devolucion.alIniciarDevolucion(recibido);
        }
    }
}
