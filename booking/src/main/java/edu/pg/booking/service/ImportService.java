package edu.pg.booking.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.pg.booking.domain.ReservationDeletedEvent;
import edu.pg.booking.domain.ReservationUpdatedEvent;
import edu.pg.booking.dto.OfferDto;
import edu.pg.booking.dto.ReservationDto;
import edu.pg.booking.dto.RollbackCommand;
import edu.pg.booking.dto.TransactionInfoDto;
import edu.pg.booking.entity.EventType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImportService {

    private final EventStoreService eventStoreService;
    private final ReservationService reservationService;
    private final RabbitTemplate rabbitTemplate;
    private final Logger logger = LoggerFactory.getLogger(ImportService.class);
    @Value("${rabbit.transaction.rollback.exchange}")
    private String exchange;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @RabbitListener(queues = "transaction.queue")
    public void handleImport(Message request) throws JsonProcessingException {
        String transactionId = "";
        try {
            TransactionInfoDto transactionInfo = objectMapper.readValue(request.getBody(), TransactionInfoDto.class);
            transactionId = transactionInfo.transactionId();
            List<ReservationDto> all = reservationService.getAllReservations();
            Map<Long, List<ReservationDto>> offerReservationMap = all.stream().collect(Collectors.groupingBy(ReservationDto::offerId));
            List<ReservationDeletedEvent> reservationsToDelete = new ArrayList<>();

            transactionInfo.deletedOffers().forEach(offerId -> {
                List<ReservationDto> reservations = offerReservationMap.get(offerId);

                if (reservations == null) {
                    return;
                }

                List<ReservationDeletedEvent> partialToDelete = reservations.stream()
                        .map(r -> new ReservationDeletedEvent(r.id(), EventType.RESERVATION_DELETED))
                        .toList();
                reservationsToDelete.addAll(partialToDelete);
            });

            Map<Long, Long> offerReservationCounts = all.stream().collect(Collectors.groupingBy(ReservationDto::offerId, Collectors.counting()));
            Map<Long, OfferDto> updatesMap = transactionInfo.updatedOffers().stream().collect(
                    Collectors.toMap(OfferDto::id, offerDto -> offerDto)
            );

            logger.info("Updated {}", transactionInfo.updatedOffers());

            Map<Long, Long> toDeleteMap = updatesMap.keySet().stream().map(id -> {
                int newMaxSeats = updatesMap.get(id).maxSeats();
                long currentCount = offerReservationCounts.get(id) == null ? 0 : offerReservationCounts.get(id);
                return Map.entry(id, Math.max(currentCount - newMaxSeats, 0));
            }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            toDeleteMap.keySet().forEach(id -> {
                long countToDelete = toDeleteMap.get(id);
                List<ReservationDto> reservations = offerReservationMap.get(id);

                if (reservations == null) {
                    return;
                }

                List<ReservationDto> sortedReservations = reservations.stream()
                        .sorted(Comparator.comparing(ReservationDto::reservationTime).reversed())
                        .toList();
                List<ReservationDeletedEvent> toDelete = sortedReservations.stream()
                        .limit(countToDelete)
                        .map(r -> new ReservationDeletedEvent(r.id(), EventType.RESERVATION_DELETED))
                        .toList();

                reservationsToDelete.addAll(toDelete);
            });

            List<ReservationUpdatedEvent> reservationsToUpdate = new ArrayList<>();

            transactionInfo.updatedOffers().forEach(update -> {
                Double newCost = update.cost();
                Date newStartTime = update.startTime();
                Date modifiedDate = new Date();

                List<ReservationDto> reservations = offerReservationMap.get(update.id());

                if (reservations == null) {
                    return;
                }

                reservations.forEach(r -> {
                    Date previousStartTime = r.startTime();
                    Integer delay = (int) (newStartTime.getTime() - previousStartTime.getTime()) / 60000;
                    ReservationUpdatedEvent event = new ReservationUpdatedEvent(r.id(), delay, newCost, modifiedDate, EventType.RESERVATION_UPDATED);
                    reservationsToUpdate.add(event);
                });
            });

            reservationsToUpdate.forEach(eventStoreService::saveEvent);
            reservationsToDelete.forEach(eventStoreService::saveEvent);

        } catch (Exception e) {
            logger.error("Error while importing data", e);
            RollbackCommand rollbackCommand = new RollbackCommand(transactionId);
            String rollbackPayload = objectMapper.writeValueAsString(rollbackCommand);
            rabbitTemplate.convertAndSend(exchange, "", rollbackPayload);
        }
    }

}
