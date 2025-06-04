package edu.pg.to_import.dto;

import java.util.List;

public record ImportCommand(
        List<OfferInsertCommand> toCreate,
        List<OfferUpdateCommand> toUpdate,
        List<Long> toDelete
) {
}
