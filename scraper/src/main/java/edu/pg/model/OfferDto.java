package edu.pg.model;

import java.time.Instant;
import java.util.List;

public record OfferDto(
        String src,
        String dest,
        Instant startTime,
        Instant endTime,
        Double cost,
        List<VehicleDto> vehicles,
        VehicleType type
) {}
