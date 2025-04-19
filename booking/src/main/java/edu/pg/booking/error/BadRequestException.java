package edu.pg.booking.error;

public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super("Bad request: %s".formatted(message));
    }
}
