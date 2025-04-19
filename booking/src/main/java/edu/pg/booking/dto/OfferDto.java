package edu.pg.booking.dto;

import java.util.Date;
import java.util.List;

public record OfferDto(
        String src,
        String dest,
        Date startTime,
        Date endTime,
        Double cost,
        List<VehicleDto> vehicles,
        VehicleType type,
        Integer availableSeats
) {
    public OfferDto(
            String src,
            String dest,
            Date startTime,
            Date endTime,
            Double cost,
            List<VehicleDto> vehicles,
            VehicleType type
    ) {
        this(src, dest, startTime, endTime, cost, vehicles, type, 0);
    }

    public OfferDto(OfferDto offer, Integer availableSeats) {
        this(offer.src, offer.dest, offer.startTime, offer.endTime, offer.cost, offer.vehicles, offer.type, availableSeats);
    }
}
