package edu.pg.to.service;

import edu.pg.to.dto.OfferDto;
import edu.pg.to.dto.OfferInsertCommand;
import edu.pg.to.dto.OfferUpdateCommand;
import edu.pg.to.dto.QueryDto;
import edu.pg.to.model.Offer;
import edu.pg.to.repository.OfferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OfferService {
    private final OfferRepository offerRepository;

    public List<OfferDto> saveOffers(List<OfferInsertCommand> toSave) {
        return toSave.stream()
                .map(dto -> {
                    Offer entity = dto.toEntity();
                    if (entity.getMaxSeats() == null) {
                        entity.setMaxSeats(dto.type().getCapacity());
                    }
                    entity.getVehicles().forEach(v -> v.setOffer(entity));
                    return offerRepository.save(entity).toDto();
                })
                .toList();
    }

    public List<OfferDto> updateOffers(List<OfferUpdateCommand> toUpdate) {
        List<Long> ids = toUpdate.stream().map(OfferUpdateCommand::id).toList();
        Map<Long, OfferUpdateCommand> toUpdateMap = toUpdate.stream().collect(java.util.stream.Collectors.toMap(OfferUpdateCommand::id, offerUpdateCommand -> offerUpdateCommand));
        List<Offer> entitiesToUpdate = offerRepository.findAllById(ids);

        entitiesToUpdate.forEach(entity -> {
            OfferUpdateCommand command = toUpdateMap.get(entity.getId());
            Date newStartDate = Date.from(entity.getStartTime().toInstant().plus(command.delay(), ChronoUnit.MINUTES));
            Date newEndDate = Date.from(entity.getEndTime().toInstant().plus(command.delay(), ChronoUnit.MINUTES));
            entity.setStartTime(newStartDate);
            entity.setEndTime(newEndDate);
            entity.setCost(command.price());
            entity.setMaxSeats(command.maxSeats());
        });

        return offerRepository.saveAll(entitiesToUpdate).stream().map(Offer::toDto).toList();
    }

    public List<OfferDto> deleteOffers(List<Long> ids) {
        List<OfferDto> offersToDelete = offerRepository.findAllById(ids)
                .stream()
                .map(Offer::toDto)
                .toList();
        offerRepository.deleteAllById(ids);
        return offersToDelete;
    }

    public List<OfferDto> getByIds(List<Long> ids) {
        return offerRepository.findAllById(ids).stream().map(Offer::toDto).toList();
    }

    public List<OfferDto> query(QueryDto query) {
        List<Offer> all = offerRepository.findAll();
        return all.stream()
                .map(Offer::toDto)
                .filter(offer -> query.src() == null || (offer.src() != null && offer.src().toLowerCase().contains(query.src().strip().toLowerCase())))
                .filter(offer -> query.dest() == null || (offer.dest() != null && offer.dest().toLowerCase().contains(query.dest().strip().toLowerCase())))
                .filter(offer -> query.time() == null || (offer.startTime() != null && offer.startTime().after(query.time())))
                .filter(offer -> offer.startTime() != null && offer.startTime().after(new Date()))
                .filter(offer -> query.maxCost() == null || query.maxCost() >= offer.cost())
                .limit(15)
                .toList();
    }
}
