package edu.pg.model;

import java.time.Instant;

public record QueryDto(
        String src,
        String dest,
        Instant time
) {
}
