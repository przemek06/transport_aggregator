package edu.pg.booking_event.consumer;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String BOOKING_EVENTS_EXCHANGE = "booking.events.exchange";
    public static final String BOOKING_EVENTS_QUEUE = "booking.events.queue";

    @Bean
    public TopicExchange bookingEventsExchange() {
        return new TopicExchange(BOOKING_EVENTS_EXCHANGE);
    }

    @Bean
    public Queue bookingEventsQueue() {
        return new Queue(BOOKING_EVENTS_QUEUE);
    }

    @Bean
    public Binding binding() {
        return BindingBuilder.bind(bookingEventsQueue())
                .to(bookingEventsExchange())
                .with("booking.event.#");
    }
}