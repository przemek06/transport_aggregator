package edu.pg.query.dto;

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
) {}
