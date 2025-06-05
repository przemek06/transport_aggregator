package edu.pg.customerinsights.domain;

import edu.pg.customerinsights.entity.EventType;

public interface DomainEvent {
    Long getId();
    EventType getEventType();
}