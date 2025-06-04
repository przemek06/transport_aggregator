package edu.pg.booking.service;

import edu.pg.booking.domain.DomainEvent;
import edu.pg.booking.domain.ReservationCreatedEvent;
import edu.pg.booking.domain.ReservationUpdatedEvent;
import edu.pg.booking.domain.VehicleInfo;
import edu.pg.booking.dto.*;
import edu.pg.booking.entity.EventType;
import edu.pg.booking.error.BadRequestException;
import edu.pg.booking.error.NotEnoughSeatsException;
import edu.pg.booking.user.CurrentUser;
import edu.pg.booking.util.EventHelper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {
    
    private final EventStoreService eventStoreService;
    private final Sinks.Many<ReservationUpdateDto> updatesSink = Sinks.many().multicast().onBackpressureBuffer();
    private final ConnectableFlux<ReservationUpdateDto> connectableUpdates = updatesSink.asFlux().publish();

    @PostConstruct
    public void init() {
        connectableUpdates.connect();
        publishUpdates();
        log.info("Reservation service initialized");
    }

    @Lookup
    protected CurrentUser getCurrentUser() {
        return null;
    }

    @Transactional
    public ReservationDto makeReservation(OfferDto offer) {
        if (offer == null || offer.vehicles() == null || offer.vehicles().isEmpty()) {
            throw new BadRequestException("Offer DTO %s in bad format".formatted(offer));
        }

        for (VehicleDto vehicleDto : offer.vehicles()) {
            int availableSeats = getAvailableSeats(offer);

            if (availableSeats <= 0) {
                throw new NotEnoughSeatsException("Vehicle with ID %s does not have enough capacity seats left".formatted(vehicleDto.id()));
            }
        }

        List<VehicleInfo> vehicles = new ArrayList<>();

        for (VehicleDto vehicleDto : offer.vehicles()) {
            VehicleInfo vehicleInfo = new VehicleInfo(
              vehicleDto.id(), vehicleDto.start(), vehicleDto.end()
            );
            vehicles.add(vehicleInfo);
        }

        ReservationCreatedEvent reservation = new ReservationCreatedEvent();
        reservation.setId(new Random().nextLong(Long.MAX_VALUE));
        reservation.setVehicles(vehicles);
        reservation.setUsername(getCurrentUser().getUsername());
        reservation.setSrc(offer.src());
        reservation.setDest(offer.dest());
        reservation.setStartTime(offer.startTime());
        reservation.setEndTime(offer.endTime());
        reservation.setCost(offer.cost());
        reservation.setOfferId(offer.id());

        eventStoreService.saveEvent(reservation);

        return ReservationDto.create(reservation, reservation.getCost(), 0);
    }

    public List<ReservationDto> getAllReservations() {
        Map<Long, List<DomainEvent>> groupedEvents = eventStoreService.getEvents().stream()
                .collect(Collectors.groupingBy(DomainEvent::getId));

        return groupedEvents.keySet().stream()
                .filter(k -> !EventHelper.isReservationDeleted(groupedEvents.get(k)))
                .map(k -> {
                    List<DomainEvent> events = groupedEvents.get(k);
                    ReservationCreatedEvent created = EventHelper.resolve(events);
                    List<ReservationUpdatedEvent> updates = EventHelper.resolveUpdates(events);
                    Double cost = updates.isEmpty() ? created.getCost() : updates.getFirst().getCost();
                    Integer delay = updates.isEmpty() ? 0 : updates.getFirst().getDelay();
                    return ReservationDto.create(created, cost, delay);
                })
                .toList();
    }

    private List<VehicleInfo> getVehicleInfo(String vehicleId) {
        Map<Long, List<DomainEvent>> groupedEvents = eventStoreService.getEvents().stream()
                .collect(Collectors.groupingBy(DomainEvent::getId));

        return groupedEvents.keySet().stream()
                .filter(k -> !EventHelper.isReservationDeleted(groupedEvents.get(k)))
                .flatMap(k -> {
                    List<DomainEvent> events = groupedEvents.get(k);
                    ReservationCreatedEvent created = EventHelper.resolve(events);
                    List<ReservationUpdatedEvent> updates = EventHelper.resolveUpdates(events);
                    Integer delay = updates.isEmpty() ? 0 : updates.getFirst().getDelay();
                    return created.getVehicles().stream()
                            .map(e -> VehicleInfo.create(e, delay));
                })
                .filter(info -> vehicleId.equals(info.vehicleId()))
                .toList();
    }

    public List<OfferDto> getAvailableSeats(List<OfferDto> offers) {
        if (offers == null || offers.isEmpty()) {
            throw new BadRequestException("Offers in bad format");
        }

        return offers.stream()
                .map(o -> {
                    Integer seats = getAvailableSeats(o);
                    return new OfferDto(o, seats);
                })
                .toList();
    }

    private Integer getAvailableSeats(OfferDto offer) {
        int minAvailableSeats = Integer.MAX_VALUE;
        for (VehicleDto vehicleDto : offer.vehicles()) {
            int capacity = offer.maxSeats();
            int occupiedSeats = (int) getVehicleInfo(vehicleDto.id()).stream()
                    .filter(info -> info.endTime().after(vehicleDto.start()) && info.startTime().before(vehicleDto.end()))
                    .count();

            int availableSeats = capacity - occupiedSeats;
            minAvailableSeats = Math.min(minAvailableSeats, availableSeats);
        }

        return minAvailableSeats;
    }

    private ReservationDto getReservation(Long id) {
        return getAllReservations().stream().filter(r -> r.id().equals(id)).findFirst().orElse(null);
    }

    private void publishUpdates() {
        eventStoreService.getEventsFlux().subscribe(event -> {
            if (event.getEventType().equals(EventType.RESERVATION_CREATED)) {
                ReservationDto reservation = getReservation(event.getId());
                ReservationUpdateDto dto = new ReservationUpdateDto(reservation, null, null, ImportOperation.CREATE);
                updatesSink.emitNext(dto, Sinks.EmitFailureHandler.FAIL_FAST);
            } else if (event.getEventType().equals(EventType.RESERVATION_UPDATED)) {
                ReservationDto reservation = getReservation(event.getId());
                ReservationUpdateDto dto = new ReservationUpdateDto(null, reservation, null, ImportOperation.UPDATE);
                updatesSink.emitNext(dto, Sinks.EmitFailureHandler.FAIL_FAST);
            } else {
                ReservationUpdateDto dto = new ReservationUpdateDto(null, null, event.getId(), ImportOperation.DELETE);
                updatesSink.emitNext(dto, Sinks.EmitFailureHandler.FAIL_FAST);
            }
        });
    }

    public Flux<ReservationUpdateDto> getUpdates() {
        return connectableUpdates;
    }

}