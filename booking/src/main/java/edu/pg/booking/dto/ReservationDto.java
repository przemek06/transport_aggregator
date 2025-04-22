package edu.pg.booking.dto;

import java.time.Instant;
import java.util.Date;

public record ReservationDto(
         Long id,
         String username,
         Date startTime,
         Date endTime,
         String src,
         String dest,
         Instant reservationTime
) {
}
