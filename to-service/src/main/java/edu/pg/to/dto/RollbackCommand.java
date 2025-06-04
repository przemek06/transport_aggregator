package edu.pg.to.dto;

public record RollbackCommand(
        String transactionId
) {
}
