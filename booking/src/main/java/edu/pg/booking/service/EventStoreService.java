package edu.pg.booking.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.pg.booking.domain.DomainEvent;
import edu.pg.booking.domain.ReservationCreatedEvent;
import edu.pg.booking.domain.ReservationDeletedEvent;
import edu.pg.booking.domain.ReservationUpdatedEvent;
import edu.pg.booking.entity.EventEntity;
import edu.pg.booking.entity.EventType;
import edu.pg.booking.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventStoreService {
    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Sinks.Many<DomainEvent> eventSink = Sinks.many().multicast().directBestEffort();

    public void saveEvent(DomainEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            EventEntity entity = new EventEntity();
            entity.setEventType(event.getEventType());
            entity.setPayload(payload);
            eventRepository.save(entity);
            eventSink.emitNext(event, Sinks.EmitFailureHandler.FAIL_FAST);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public List<DomainEvent> getEvents() {
        return eventRepository.findAll().stream()
                .map(this::deserializeEvent)
                .collect(Collectors.toList());
    }

    // USE THIS TO GET A STREAM OF EVENTS. SUBSCRIBE TO THE FLUX WHERE YOU NEED IT
    // AND USE RABBITMQ TO DYNAMICALLY SEND MESSAGES TO NOTIFICATION SERVICES
    public Flux<DomainEvent> getEventsFlux() {
        return eventSink.asFlux();
    }

    private DomainEvent deserializeEvent(EventEntity entity) {
        try {
            Class<?> clazz = switch (entity.getEventType()) {
                case EventType.RESERVATION_CREATED -> ReservationCreatedEvent.class;
                case EventType.RESERVATION_UPDATED -> ReservationUpdatedEvent.class;
                case EventType.RESERVATION_DELETED -> ReservationDeletedEvent.class;
            };
            return (DomainEvent) objectMapper.readValue(entity.getPayload(), clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
