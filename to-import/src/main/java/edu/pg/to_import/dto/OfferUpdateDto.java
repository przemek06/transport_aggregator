package edu.pg.to_import.dto;

public record OfferUpdateDto(
        OfferInsertCommand inserted,
        OfferUpdateCommand updated,
        Long deleted,
        ImportOperation operation
) {
}
