package edu.pg.booking.controller;


import edu.pg.booking.dto.OfferDto;
import edu.pg.booking.dto.ReservationDto;
import edu.pg.booking.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final Logger logger = LoggerFactory.getLogger(ReservationController.class);

    @PostMapping
    public ResponseEntity<ReservationDto> makeReservation(@RequestBody OfferDto offer) {
        logger.info("Received makeReservation request %s".formatted(offer));
        ReservationDto reservation = reservationService.makeReservation(offer);
        return ResponseEntity.ok(reservation);
    }

    @GetMapping("/all")
    public ResponseEntity<List<ReservationDto>> getAllReservations() {
        logger.info("Received getFutureReservations request");
        List<ReservationDto> results = reservationService.getAllReservations();
        return ResponseEntity.ok(results);
    }

    @PostMapping("/available-seats")
    public ResponseEntity<List<OfferDto>> getAvailableSeats(@RequestBody List<OfferDto> offers) {
        logger.info("Received getAvailableSeats request %s".formatted(offers));
        List<OfferDto> result = reservationService.getAvailableSeats(offers);
        return ResponseEntity.ok(result);
    }


    @GetMapping("/updates")
    public SseEmitter getUpdates() {
        SseEmitter emitter = new SseEmitter(-1L);

        reservationService.getUpdates().subscribe(u -> {
            try {
                emitter.send(u);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        return emitter;
    }
}