package edu.pg.to.service;

import edu.pg.to.dto.OfferDto;
import edu.pg.to.dto.QueryDto;
import edu.pg.to.model.Offer;
import edu.pg.to.repository.OfferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OfferService {
    private final OfferRepository offerRepository;

    public List<OfferDto> saveOffers(List<OfferDto> toSave) {
        return toSave.stream()
                .map(dto -> {
                    Offer entity = dto.toEntity();
                    if (entity.getMaxSeats() == null) {
                        entity.setMaxSeats(dto.type().getCapacity());
                        entity.getVehicles().forEach(v -> v.setOffer(entity));
                    }
                    return offerRepository.save(entity).toDto();
                })
                .toList();
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
