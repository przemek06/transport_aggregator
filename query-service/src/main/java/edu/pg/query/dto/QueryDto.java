package edu.pg.query.dto;

import java.util.Date;

public record QueryDto(
        String src,
        String dest,
        Date time,
        Double maxCost
) {
}
