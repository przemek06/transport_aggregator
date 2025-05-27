package edu.pg.booking.dto;

import java.util.Date;
import java.util.List;

public record OfferDto(
        Long id,
        String src,
        String dest,
        Date startTime,
        Date endTime,
        Double cost,
        List<VehicleDto> vehicles,
        VehicleType type,
        Integer availableSeats,
        Integer maxSeats
) {
    public OfferDto(OfferDto offer, Integer availableSeats) {
        this(offer.id, offer.src, offer.dest, offer.startTime, offer.endTime, offer.cost, offer.vehicles, offer.type, availableSeats, offer.maxSeats);
    }
}
