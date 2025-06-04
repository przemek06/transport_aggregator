package edu.pg.to.dto;

import edu.pg.to.model.Offer;
import lombok.Builder;

import java.time.temporal.ChronoUnit;
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
) {
    public Offer toEntity() {
        return new Offer(null, this.src(), this.dest(), this.startTime(), this.endTime(), this.cost(), this.type(), this.maxSeats(), this.vehicles().stream().map(VehicleDto::toEntity).toList());
    }

    public OfferInsertCommand update(Integer daysToAdd) {
        Date newStartTime = Date.from(startTime.toInstant().plus(daysToAdd, ChronoUnit.DAYS));
        Date newEndTime = Date.from(endTime.toInstant().plus(daysToAdd, ChronoUnit.DAYS));
        return new OfferInsertCommand(src, dest, newStartTime, newEndTime, cost, vehicles, type, maxSeats);
    }
}
