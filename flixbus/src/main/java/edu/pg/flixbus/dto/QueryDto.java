package edu.pg.flixbus.dto;

import java.util.Date;

public record QueryDto(
        String src,
        String dest,
        Date time
) {
}
