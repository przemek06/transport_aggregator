package edu.pg.booking_event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.pg.booking_event.domain.DomainEvent;
import edu.pg.booking_event.domain.ReservationCreatedEvent;
import edu.pg.booking_event.consumer.RabbitConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Flux;

@Service
public class BookingEventConsumer {

//    private final RabbitTemplate rabbitTemplate;
    private static final Logger log = LoggerFactory.getLogger(BookingEventConsumer.class);
    private final Sinks.Many<ReservationCreatedEvent> eventSink = Sinks.many().multicast().onBackpressureBuffer();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @RabbitListener(queues = RabbitConfig.BOOKING_EVENTS_QUEUE)
    public void consumeEvent(String json) {
        try {
            ReservationCreatedEvent event = objectMapper.readValue(json, ReservationCreatedEvent.class);
            log.debug("Received domain event: {}", event);
            var result = eventSink.tryEmitNext(event);

            if (result.isFailure()) {
                log.warn("Failed to emit event {}: {}", event, result);
            }
        } catch (Exception e) {
            log.error("Error parsing event JSON", e);
        }
    }

    public Flux<ReservationCreatedEvent> getEventFlux() {
        return eventSink.asFlux()
                .onErrorResume(e -> {
                    log.error("Error in booking event stream", e);
                    return Flux.empty();
                });
    }
}