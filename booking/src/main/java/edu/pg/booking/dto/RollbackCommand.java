package edu.pg.booking.dto;

public record RollbackCommand(
        String transactionId
) {
}
