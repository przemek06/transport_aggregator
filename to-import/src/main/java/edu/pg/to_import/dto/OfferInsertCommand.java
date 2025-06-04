package edu.pg.to_import.dto;

import lombok.Builder;

import java.util.Date;
import java.util.List;

@Builder
public record OfferInsertCommand(
        String src,
        String dest,
        Date startTime,
        Date endTime,
        Double cost,
        List<VehicleDto> vehicles,
        VehicleType type,
        Integer maxSeats
) {}
