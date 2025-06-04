package edu.pg.to.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.pg.to.dto.*;
import edu.pg.to.model.Offer;
import edu.pg.to.model.RollbackInfo;
import edu.pg.to.model.TransactionLog;
import edu.pg.to.repository.OfferRepository;
import edu.pg.to.repository.TransactionLogRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ImportService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OfferService offerService;
    private final OfferRepository offerRepository;
    private final TransactionLogRepository transactionLogRepository;
    private final RabbitTemplate rabbitTemplate;
    @Value("${rabbit.transaction.exchange}")
    private String exchange;
    private final Logger logger = LoggerFactory.getLogger(ImportService.class);

    @RabbitListener(queues = "import.queue")
    public void handleRequest(Message request) {
        try {
            ImportCommand importCommand = objectMapper.readValue(request.getBody(), ImportCommand.class);
            RollbackInfo rollbackInfo = getRollbackInfo(importCommand);
            List<OfferInsertCommand> insertCommands = importCommand.toCreate();
            List<OfferUpdateCommand> updateCommands = importCommand.toUpdate();
            List<Long> deleteCommands = importCommand.toDelete();

            List<OfferDto> created = offerService.saveOffers(insertCommands);
            List<OfferDto> updated = offerService.updateOffers(updateCommands);
            List<OfferDto> deleted = offerService.deleteOffers(deleteCommands);

            String transactionId = UUID.randomUUID().toString();
            TransactionInfoDto transactionInfo = new TransactionInfoDto(transactionId, created, updated, deleteCommands);
            String transactionInfoPayload = objectMapper.writeValueAsString(transactionInfo);
            logger.info("Transaction info = {}", transactionInfoPayload);

            rollbackInfo = updateRollbackInfo(rollbackInfo, created);
            String rollbackInfoPayload = objectMapper.writeValueAsString(rollbackInfo);
            TransactionLog transactionLog = new TransactionLog();
            transactionLog.setTransactionId(transactionId);
            transactionLog.setData(rollbackInfoPayload);
            transactionLogRepository.save(transactionLog);

            rabbitTemplate.convertAndSend(exchange, "", transactionInfoPayload.getBytes());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @RabbitListener(queues = "transaction.rollback.queue")
    public void handleRollback(Message request) {
        try {
            RollbackCommand rollbackCommand = objectMapper.readValue(request.getBody(), RollbackCommand.class);
            TransactionLog transactionLog = transactionLogRepository.findByTransactionId(rollbackCommand.transactionId())
                    .orElse(null);

            if (transactionLog == null) {
                return;
            }

            RollbackInfo rollbackInfo = objectMapper.readValue(transactionLog.getData(), RollbackInfo.class);
            List<Offer> toSave = Stream.concat(rollbackInfo.toCreate().stream(), rollbackInfo.toUpdate().stream()).toList();
            List<Long> toDelete = rollbackInfo.toDelete();

            offerRepository.saveAll(toSave);
            offerRepository.deleteAllById(toDelete);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private RollbackInfo getRollbackInfo(ImportCommand importCommand) {
        List<Offer> toRestore = offerService.getByIds(importCommand.toDelete()).stream()
                .map(OfferDto::toEntity)
                .toList();

        List<Offer> toUpdate = offerService.getByIds(importCommand.toUpdate().stream().map(OfferUpdateCommand::id).toList()).stream()
                .map(OfferDto::toEntity)
                .toList();

        return new RollbackInfo(toRestore, toUpdate, null);
    }

    private RollbackInfo updateRollbackInfo(RollbackInfo rollbackInfo, List<OfferDto> created) {
        List<Long> toDelete = created.stream().map(OfferDto::id).toList();
        return new RollbackInfo(rollbackInfo.toCreate(), rollbackInfo.toUpdate(), toDelete);
    }
}
