package edu.pg.booking.error;

public class NotEnoughSeatsException extends RuntimeException {

    public NotEnoughSeatsException(String message) {
        super("Not enough seats: %s".formatted(message));
    }
}
