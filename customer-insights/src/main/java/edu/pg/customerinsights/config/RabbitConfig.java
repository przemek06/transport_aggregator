package edu.pg.customerinsights.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    // Match the routing key as used in publisher code!
    public static final String QUEUE = "customer.insights.queue";
    public static final String EXCHANGE = "booking.events.exchange";
    public static final String ROUTING_KEY = "booking.event.reservationcreatedevent";

    @Bean
    public Queue reservationCreatedQueue() {
        return new Queue(QUEUE, true); // durable queue
    }

    @Bean
    public TopicExchange bookingExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Binding reservationCreatedBinding() {
        return BindingBuilder.bind(reservationCreatedQueue())
                .to(bookingExchange())
                .with(ROUTING_KEY);
    }
}
