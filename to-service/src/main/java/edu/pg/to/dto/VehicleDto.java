package edu.pg.to.dto;

import edu.pg.to.model.Vehicle;

import java.util.Date;

public record VehicleDto(
        String id,
        Date start,
        Date end
) {
    public Vehicle toEntity() {
        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleId(id);
        vehicle.setStartDate(start);
        vehicle.setEndDate(end);
        return vehicle;
    }
}
