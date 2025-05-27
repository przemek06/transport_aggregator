package edu.pg.to.dto;

import lombok.Getter;

@Getter
public enum VehicleType {
    TRAIN(200), BUS(50);

    VehicleType(Integer capacity) {
        this.capacity = capacity;
    }

    private final Integer capacity;
}
