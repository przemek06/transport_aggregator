package edu.pg.booking_event.domain;

import edu.pg.booking_event.entity.EventType;

public interface DomainEvent {
    Long getId();
    EventType getEventType();
}