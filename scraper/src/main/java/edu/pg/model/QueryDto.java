package edu.pg.model;

import java.util.Date;

public record QueryDto(
        String src,
        String dest,
        Date time
) {
}
