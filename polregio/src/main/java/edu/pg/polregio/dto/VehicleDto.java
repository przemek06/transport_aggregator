package edu.pg.polregio.dto;

import java.util.Date;

public record VehicleDto(
        String id,
        Date start,
        Date end
) {
}
