package edu.pg.booking_event.domain;

import edu.pg.booking_event.entity.EventType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationDeletedEvent implements DomainEvent {
    private Long id;
    private EventType eventType = EventType.RESERVATION_DELETED;
}
