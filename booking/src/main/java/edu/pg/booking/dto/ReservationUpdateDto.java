package edu.pg.booking.dto;

public record ReservationUpdateDto(
        ReservationDto inserted,
        ReservationDto updated,
        Long deleted,
        ImportOperation operation
) {
}
