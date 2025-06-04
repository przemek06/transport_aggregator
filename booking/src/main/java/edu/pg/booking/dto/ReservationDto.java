package edu.pg.booking.dto;

import edu.pg.booking.domain.ReservationCreatedEvent;

import java.time.temporal.ChronoUnit;
import java.util.Date;

public record ReservationDto(
         Long id,
         String username,
         Date startTime,
         Date endTime,
         String src,
         String dest,
         Double cost,
         Date reservationTime,
         Long offerId
) {

    public static ReservationDto create(ReservationCreatedEvent event, Double cost, Integer delay) {
        Date startTime = Date.from(event.getStartTime().toInstant().plus(delay, ChronoUnit.MINUTES));
        Date endTime = Date.from(event.getEndTime().toInstant().plus(delay, ChronoUnit.MINUTES));

        return new ReservationDto(
                event.getId(),
                event.getUsername(),
                startTime,
                endTime,
                event.getSrc(),
                event.getDest(),
                cost,
                event.getReservationTime(),
                event.getOfferId()
        );
    }
}
