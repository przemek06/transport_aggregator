package edu.pg.customerinsights.domain;

import edu.pg.customerinsights.entity.EventType;
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
