package edu.pg.query.dto;

import java.util.Date;

public record OfferDto(
        String src,
        String dest,
        Date time,
        Double cost
) {
}
