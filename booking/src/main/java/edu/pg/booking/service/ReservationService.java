package edu.pg.booking.service;

import edu.pg.booking.dto.OfferDto;
import edu.pg.booking.dto.ReservationDto;
import edu.pg.booking.dto.VehicleDto;
import edu.pg.booking.entity.Reservation;
import edu.pg.booking.entity.VehicleReservation;
import edu.pg.booking.error.BadRequestException;
import edu.pg.booking.error.NotEnoughSeatsException;
import edu.pg.booking.repository.ReservationRepository;
import edu.pg.booking.repository.VehicleReservationRepository;
import edu.pg.booking.user.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final VehicleReservationRepository vehicleReservationRepository;

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
            int capacity = offer.type().getCapacity();
            int occupiedSeats = vehicleReservationRepository.findByVehicleIdAndEndTimeGreaterThanEqualAndStartTimeLessThanEqual(
                            vehicleDto.id(),
                            vehicleDto.start(),
                            vehicleDto.end())
                    .size();

            if (occupiedSeats >= capacity) {
                throw new NotEnoughSeatsException("Vehicle with ID %s does not have enough capacity seats left".formatted(vehicleDto.id()));
            }
        }

        Reservation reservation = new Reservation();
        reservation.setUsername(getCurrentUser().getUsername());
        reservation.setStartTime(offer.startTime());
        reservation.setEndTime(offer.endTime());
        reservation.setReservationTime(Instant.now());
        reservation.setSrc(offer.src());
        reservation.setDest(offer.dest());

        reservationRepository.save(reservation);

        for (VehicleDto vehicleDto : offer.vehicles()) {
            VehicleReservation vehicleReservation = new VehicleReservation();
            vehicleReservation.setVehicleId(vehicleDto.id());
            vehicleReservation.setStartTime(vehicleDto.start());
            vehicleReservation.setEndTime(vehicleDto.end());
            vehicleReservation.setReservation(reservation);
            vehicleReservationRepository.save(vehicleReservation);
        }

        return reservation.toDto();
    }

    public List<ReservationDto> getFutureReservations() {
        Date now = new Date();
        return reservationRepository.findByEndTimeAfter(now)
                .stream()
                .map(Reservation::toDto)
                .sorted(Comparator.comparing(ReservationDto::reservationTime))
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
            int occupiedSeats = vehicleReservationRepository.findByVehicleIdAndEndTimeGreaterThanEqualAndStartTimeLessThanEqual(
                            vehicleDto.id(),
                            vehicleDto.start(),
                            vehicleDto.end())
                    .size();

            int availableSeats = capacity - occupiedSeats;
            minAvailableSeats = Math.min(minAvailableSeats, availableSeats);
        }

        return minAvailableSeats;
    }
}