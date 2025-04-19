package edu.pg.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum VehicleType {
    TRAIN(200), BUS(50);

    final int capacity;
}
