package edu.pg.query.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.pg.query.client.RPCClient;
import edu.pg.query.dto.OfferDto;
import edu.pg.query.dto.QueryDto;
import edu.pg.query.dto.TransactionInfoDto;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OfferService {

    private final RPCClient rpcClient;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Logger logger = LoggerFactory.getLogger(OfferService.class);

    private final Sinks.Many<TransactionInfoDto> updatesSink = Sinks.many().multicast().onBackpressureBuffer();
    private final ConnectableFlux<TransactionInfoDto> connectableUpdates = updatesSink.asFlux().publish();

    @PostConstruct
    public void init() {
        connectableUpdates.connect();
    }

    public Flux<List<OfferDto>> getOffers(String src, String dest, Date time, Double maxCost) {
        QueryDto query = new QueryDto(src, dest, time, maxCost);
        return rpcClient.request(query);
    }

    public Flux<TransactionInfoDto> getUpdates() {
        return connectableUpdates;
    }

    @RabbitListener(queues = "transaction.query.queue")
    public void handleImport(Message request) {
        try {
            TransactionInfoDto update = objectMapper.readValue(request.getBody(), TransactionInfoDto.class);
            updatesSink.emitNext(update, Sinks.EmitFailureHandler.FAIL_FAST);

        } catch (Exception e) {
            logger.error("Error processing message", e);
        }
    }

}
