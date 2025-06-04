package edu.pg.to_import.dto;

public record OfferUpdateCommand(
        Long id,
        Double price,
        Integer delay,
        Integer maxSeats
) {
}
