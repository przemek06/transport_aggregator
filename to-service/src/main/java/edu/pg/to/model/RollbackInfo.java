package edu.pg.to.model;

import java.util.List;

public record RollbackInfo (
        List<Offer> toCreate,
        List<Offer> toUpdate,
        List<Long> toDelete
){
}
