package edu.pg.booking.dto;

import java.util.List;

public record TransactionInfoDto(
        String transactionId,
        List<OfferDto> createdOffers,
        List<OfferDto> updatedOffers,
        List<Long> deletedOffers
) {
}
