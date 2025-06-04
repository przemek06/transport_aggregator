package edu.pg.booking.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.pg.booking.entity.EventType;
import edu.pg.booking.domain.DomainEvent;
import edu.pg.booking.service.EventStoreService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DomainEventPublisher.class);
    public static final String BOOKING_EVENTS_EXCHANGE = "booking.events.exchange";
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final RabbitTemplate rabbitTemplate;
    private final EventStoreService eventStoreService;

    public DomainEventPublisher (RabbitTemplate rabbitTemplate,
                                 EventStoreService eventStoreService) {
        this.rabbitTemplate = rabbitTemplate;
        this.eventStoreService = eventStoreService;
        subscribeToEvents();
    }

    private void subscribeToEvents() {
        Flux<DomainEvent> eventFlux = eventStoreService.getEventsFlux();

        eventFlux
                .filter(event -> event.getEventType() == EventType.RESERVATION_CREATED) // only created events
                .subscribe(
                        event -> {
                            try {
                                rabbitTemplate.convertAndSend(
                                        BOOKING_EVENTS_EXCHANGE,
                                        "booking.event." + event.getClass().getSimpleName().toLowerCase(),
                                        objectMapper.writeValueAsString(event));
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException(e);
                            }
                        },
                        error -> {
                            log.error("Error in booking event subscription", error);
                        }
                );
    }
}