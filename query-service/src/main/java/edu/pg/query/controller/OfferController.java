package edu.pg.query.controller;

import edu.pg.query.service.OfferService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@RestController()
@RequestMapping("/query/offers")
@RequiredArgsConstructor
public class OfferController {

    private final OfferService offerService;
    private Logger logger = LoggerFactory.getLogger(OfferController.class);

    @GetMapping("/src/{src}/dest/{dest}/time/{time}")
    public SseEmitter getOffers(@PathVariable String src,
                                @PathVariable String dest,
                                @PathVariable String time) {
        logger.info("Request received");
        SseEmitter emitter = new SseEmitter(10000L);

        emitter.onTimeout(() -> {
            logger.info("SSE emitter timeout");
        });

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        LocalDateTime dateTime = LocalDateTime.parse(time, formatter);

        offerService.getOffers(src, dest, Date.from(dateTime.atZone(ZoneId.of("Europe/Warsaw")).toInstant()))
                .doOnNext(resp -> {
                    try {
                        emitter.send(resp);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .onErrorComplete()
                .doOnComplete(() -> {
                    logger.info("SSE request flux completed");
                })
                .subscribe();

        return emitter;
    }
}