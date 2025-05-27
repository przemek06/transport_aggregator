package edu.pg.to.dto;

import java.util.Date;

public record QueryDto(
        String src,
        String dest,
        Date time,
        Double maxCost
) {
}
