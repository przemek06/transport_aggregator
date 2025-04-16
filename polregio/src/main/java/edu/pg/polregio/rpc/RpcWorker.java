package edu.pg.polregio.rpc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.pg.polregio.dto.OfferDto;
import edu.pg.polregio.dto.QueryDto;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RpcWorker {

    private static final Logger logger = LoggerFactory.getLogger(RpcWorker.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebScraper webScraper;

    @RabbitListener(queues = "rpc.queue.polregio")
    public void handleRequest(Message request) throws IOException {
        String correlationId = request.getMessageProperties().getCorrelationId();

        try {
            logger.info("Received request with correlationId={}", correlationId);

            QueryDto query = objectMapper.readValue(request.getBody(), QueryDto.class);
            List<OfferDto> response = webScraper.getOffers(query);
            String responsePayload = objectMapper.writeValueAsString(response);

            MessageProperties responseProperties = new MessageProperties();
            responseProperties.setCorrelationId(correlationId);
            Message responseMessage = new Message(responsePayload.getBytes(), responseProperties);

            logger.info("Responding to={}", request.getMessageProperties().getReplyTo());

            rabbitTemplate.send(request.getMessageProperties().getReplyTo(), responseMessage);

        } catch (JsonProcessingException e) {
            logger.error("Error processing the request", e);
            throw new RuntimeException(e);
        }
    }
}
