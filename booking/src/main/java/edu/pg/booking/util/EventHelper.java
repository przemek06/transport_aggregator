package edu.pg.booking.util;

import edu.pg.booking.domain.DomainEvent;
import edu.pg.booking.domain.ReservationCreatedEvent;
import edu.pg.booking.domain.ReservationUpdatedEvent;
import edu.pg.booking.entity.EventType;

import java.util.Comparator;
import java.util.List;

public class EventHelper {
    public static ReservationCreatedEvent resolve(List<DomainEvent> events) {
        return events.stream()
                .filter(e -> EventType.RESERVATION_CREATED.equals(e.getEventType()))
                .map(e -> (ReservationCreatedEvent) e)
                .findFirst()
                .orElse(null);
    }

    public static boolean isReservationDeleted(List<DomainEvent> events) {
        return events.stream()
                .anyMatch(e -> EventType.RESERVATION_DELETED.equals(e.getEventType()));
    }

    public static List<ReservationUpdatedEvent> resolveUpdates(List<DomainEvent> events) {
        return events.stream()
                .filter(e -> EventType.RESERVATION_UPDATED.equals(e.getEventType()))
                .map(e -> (ReservationUpdatedEvent) e)
                .sorted(Comparator.comparing(ReservationUpdatedEvent::getModifiedAt))
                .toList();
    }
}
