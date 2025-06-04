package edu.pg.customerinsights.domain;

import java.time.temporal.ChronoUnit;
import java.util.Date;

public record VehicleInfo(
        String vehicleId,
        Date startTime,
        Date endTime
) {
    public static VehicleInfo create(VehicleInfo e, Integer delay) {
        Date startTime = Date.from(e.startTime.toInstant().plus(delay, ChronoUnit.MINUTES));
        Date endTime = Date.from(e.endTime.toInstant().plus(delay, ChronoUnit.MINUTES));

        return new VehicleInfo(
                e.vehicleId,
                startTime,
                endTime
        );
    }
}
