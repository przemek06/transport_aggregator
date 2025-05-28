package edu.pg.booking.domain;

import edu.pg.booking.entity.EventType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationUpdatedEvent implements DomainEvent {
    private Long id;
    private Integer delay;
    private Double cost;
    private Date modifiedAt = new Date();
    private EventType eventType = EventType.RESERVATION_UPDATED;
}
