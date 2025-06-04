package edu.pg.booking_event.controller;

import edu.pg.booking_event.domain.ReservationCreatedEvent;
import edu.pg.booking_event.consumer.BookingEventConsumer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/stream")
@RequiredArgsConstructor
public class BookingEventController {

    private Logger logger = LoggerFactory.getLogger(BookingEventController.class);
    private final BookingEventConsumer eventConsumer;
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @GetMapping("/reservation-created")
    public SseEmitter getCreatedEvents() {
        SseEmitter emitter = new SseEmitter(0L); // no timeout
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> {
            emitters.remove(emitter);
            logger.error("SSE emitter error", e);
        });

        logger.info("New SSE client connected. Total clients: {}", emitters.size());
        return emitter;
    }

    @PostConstruct
    public void initEventStreaming() {
        Flux<ReservationCreatedEvent> flux = eventConsumer.getEventFlux();
        flux.subscribe(event -> {
            List<SseEmitter> deadEmitters = new java.util.ArrayList<>();
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("booking-event")
                            .data(event));
                } catch (IOException e) {
                    deadEmitters.add(emitter);
                }
            }
            emitters.removeAll(deadEmitters);
        });
    }
}