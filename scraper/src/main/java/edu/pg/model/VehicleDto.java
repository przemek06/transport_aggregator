package edu.pg.model;

import java.util.Date;

public record VehicleDto(
        String id,
        Date start,
        Date end
) {
}
