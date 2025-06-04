package edu.pg.to.dto;

public record OfferUpdateCommand(
        Long id,
        Double price,
        Integer delay,
        Integer maxSeats
) {
}
