package edu.pg.booking.domain;

import edu.pg.booking.entity.EventType;
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
