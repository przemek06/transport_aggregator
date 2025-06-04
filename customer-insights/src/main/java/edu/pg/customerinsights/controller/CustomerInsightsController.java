package edu.pg.customerinsights.controller;

import edu.pg.customerinsights.domain.ReservationCreatedEvent;
import edu.pg.customerinsights.consumer.CustomerInsightsConsumer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/stream")
@RequiredArgsConstructor
public class CustomerInsightsController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerInsightsController.class);

    private final CustomerInsightsConsumer eventConsumer;
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final Map<String, AtomicInteger> destinationCounter = new ConcurrentHashMap<>();

    @GetMapping("/top-destinations")
    public SseEmitter getTopDestinationsStream() {
        SseEmitter emitter = new SseEmitter(0L); // no timeout
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> {
            emitters.remove(emitter);
            logger.error("SSE emitter error", e);
        });

        // Immediately send the current top 5 when a new client connects
        try {
            emitter.send(SseEmitter.event()
                    .name("top-destinations")
                    .data(getTop5Destinations()));
        } catch (IOException e) {
            logger.warn("Failed to send initial top 5 destinations", e);
        }

        logger.info("New SSE client connected. Total clients: {}", emitters.size());
        return emitter;
    }

    // Listen to all ReservationCreatedEvents and update/populate emitters with top 5
    @PostConstruct
    public void initEventStreaming() {
        Flux<ReservationCreatedEvent> flux = eventConsumer.getEventFlux();
        flux.subscribe(event -> {
            if (event.getDest() != null && !event.getDest().isBlank()) {
                destinationCounter.computeIfAbsent(event.getDest(), k -> new AtomicInteger(0))
                        .incrementAndGet();
                List<String> top5 = getTop5Destinations();

                List<SseEmitter> deadEmitters = new ArrayList<>();
                for (SseEmitter emitter : emitters) {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("top-destinations")
                                .data(top5));
                    } catch (IOException e) {
                        deadEmitters.add(emitter);
                    }
                }
                emitters.removeAll(deadEmitters);
            }
        });
    }

    // Returns a List of top 5 destination names (most popular first)
    private List<String> getTop5Destinations() {
        return destinationCounter.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue().get(), e1.getValue().get()))
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
