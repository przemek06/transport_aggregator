package edu.pg.query.dto;

import edu.pg.polregio.dto.VehicleDto;
import edu.pg.polregio.dto.VehicleType;

import java.util.Date;
import java.util.List;

public record OfferDto(
        String src,
        String dest,
        Date startTime,
        Date endTime,
        Double cost,
        List<VehicleDto> vehicles,
        VehicleType type
) {}
