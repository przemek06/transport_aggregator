package edu.pg.booking.domain;

import edu.pg.booking.entity.EventType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationCreatedEvent implements DomainEvent {
    private Long id;
    private String username;
    private Long offerId;
    private Date startTime;
    private Date endTime;
    private String src;
    private String dest;
    private Double cost;
    private Date reservationTime = new Date();
    private List<VehicleInfo> vehicles;
    private EventType eventType = EventType.RESERVATION_CREATED;
}
