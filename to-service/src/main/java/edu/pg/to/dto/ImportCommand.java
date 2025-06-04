package edu.pg.to.dto;

import java.util.List;

public record ImportCommand(
        List<OfferInsertCommand> toCreate,
        List<OfferUpdateCommand> toUpdate,
        List<Long> toDelete
) {
}
