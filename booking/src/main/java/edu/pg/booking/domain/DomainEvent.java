package edu.pg.booking.domain;

import edu.pg.booking.entity.EventType;

public interface DomainEvent {
    Long getId();
    EventType getEventType();
}