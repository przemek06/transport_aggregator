package edu.pg.flixbus.rpc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.pg.flixbus.dto.OfferDto;
import edu.pg.flixbus.dto.QueryDto;
import lombok.RequiredArgsConstructor;
import org.openqa.selenium.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RpcWorker {

    private static final Logger logger = LoggerFactory.getLogger(RpcWorker.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebScraper webScraper;

    @RabbitListener(queues = "rpc.queue.polregio")
    public void handleRequest(Message request) {
        String correlationId = request.getMessageProperties().getCorrelationId();

        try {
            logger.info("Received request with correlationId={}", correlationId);

            QueryDto query = objectMapper.readValue(request.getBody(), QueryDto.class);
            logger.info("Query = {}", query);

            List<OfferDto> response = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                try {
                    response = webScraper.getOffers(query);
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.error("Error while scraping. Retrying: {}", i);
                }
            }
            logger.info("Response = {}", response);
            Message responseMessage = constructResponse(response, correlationId);
            logger.info("Responding to={}", request.getMessageProperties().getReplyTo());
            rabbitTemplate.send(request.getMessageProperties().getReplyTo(), responseMessage);

        } catch (IOException | TimeoutException e) {
            logger.error("Error processing the request", e);
            List<OfferDto> response = Collections.emptyList();
            Message responseMessage = constructResponse(response, correlationId);
            rabbitTemplate.send(request.getMessageProperties().getReplyTo(), responseMessage);
        }
    }

    private Message constructResponse(List<OfferDto> response, String correlationId) {
        String responsePayload = null;
        try {
            responsePayload = objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        MessageProperties responseProperties = new MessageProperties();
        responseProperties.setCorrelationId(correlationId);
        return new Message(responsePayload.getBytes(), responseProperties);
    }
}
