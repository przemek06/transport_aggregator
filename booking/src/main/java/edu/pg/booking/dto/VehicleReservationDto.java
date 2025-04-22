package edu.pg.booking.dto;

import java.util.Date;

public record VehicleReservationDto(
         Long id,
         String vehicleId,
         Date startTime,
         Date endTime
) {
}
