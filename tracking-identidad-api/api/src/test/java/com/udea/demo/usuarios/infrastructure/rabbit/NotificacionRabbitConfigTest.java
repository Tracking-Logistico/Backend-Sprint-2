package com.udea.demo.usuarios.infrastructure.rabbit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificacionRabbitConfig - configuración de colas y topología RabbitMQ")
class NotificacionRabbitConfigTest {

    private final NotificacionRabbitConfig config = new NotificacionRabbitConfig();

    @Mock private ConnectionFactory connectionFactory;
    @Mock private MessageConverter messageConverter;

    @Test
    @DisplayName("Declara colas, DLQ, reintentos y contenedor con propiedades correctas")
    void testConfiguracionColas() {
        Queue mainQueue = config.notificationQueue();
        assertThat(mainQueue.getName()).isEqualTo(NotificacionRabbitConfig.QUEUE);
        assertThat(mainQueue.isDurable()).isTrue();

        Queue retryQueue = config.notificationRetryQueue(5000);
        assertThat(retryQueue.getName()).isEqualTo(NotificacionRabbitConfig.RETRY_QUEUE);

        Queue dlq = config.notificationDlq();
        assertThat(dlq.getName()).isEqualTo(NotificacionRabbitConfig.DLQ);

        TopicExchange exchange = new TopicExchange("logistica.eventos");
        Binding binding = config.notificationBinding(mainQueue, exchange);
        assertThat(binding.getRoutingKey()).isEqualTo("pedido.#");

        SimpleRabbitListenerContainerFactory factory =
                config.notificacionListenerFactory(connectionFactory, messageConverter);
        assertThat(factory).isNotNull();
    }
}
