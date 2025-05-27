package edu.pg.to.dto;

import edu.pg.to.model.Offer;

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
        Integer maxSeats
) {
    public Offer toEntity() {
        Offer offer = new Offer();
        offer.setId(id);
        offer.setSrc(src);
        offer.setDest(dest);
        offer.setStartTime(startTime);
        offer.setEndTime(endTime);
        offer.setCost(cost);
        offer.setVehicles(vehicles.stream()
                .map(VehicleDto::toEntity).toList());
        offer.setType(type);
        offer.setMaxSeats(maxSeats);
        return offer;
    }
}
