package edu.pg.query.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.pg.query.dto.OfferDto;
import edu.pg.query.dto.QueryDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class RPCClient {

    private static final Logger logger = LoggerFactory.getLogger(RPCClient.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String exchange;
    private final DirectExchange responseExchange;
    private final String responseQueue;
    private final RabbitAdmin rabbitAdmin;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public RPCClient(RabbitTemplate rabbitTemplate,
                     @Value("${rabbit.rpc.exchange}") String exchange,
                     DirectExchange responseExchange,
                     @Value("${rabbit.rpc.response.queue}") String responseQueue,
                     RabbitAdmin rabbitAdmin) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.responseExchange = responseExchange;
        this.responseQueue = responseQueue;
        this.rabbitAdmin = rabbitAdmin;
    }

    public Flux<List<OfferDto>> request(QueryDto query) {
        return Flux.create(sink -> {
            try {
                String correlationId = UUID.randomUUID().toString();
                String replyQueue = responseQueue + correlationId;

                Queue tempQueue = new Queue(replyQueue, false, true, true);
                Binding binding = BindingBuilder.bind(tempQueue).to(responseExchange).with(replyQueue);

                rabbitAdmin.declareQueue(tempQueue);
                rabbitAdmin.declareBinding(binding);

                String payload = objectMapper.writeValueAsString(query);
                MessageProperties props = new MessageProperties();
                props.setReplyTo(replyQueue);
                props.setCorrelationId(correlationId);
                Message requestMessage = new Message(payload.getBytes(StandardCharsets.UTF_8), props);

                SimpleMessageListenerContainer container = setupResponseListener(replyQueue, sink);

                logger.info("Sent request = {}", requestMessage);
                rabbitTemplate.convertAndSend(exchange, "", requestMessage);

                scheduler.schedule(() -> {
                    sink.complete();
                    container.stop();
                    logger.info("Reply timeout");
                }, 60, TimeUnit.SECONDS);

            } catch (Exception e) {
                sink.complete();
            }
        });
    }

    private SimpleMessageListenerContainer setupResponseListener(String replyQueue, FluxSink<List<OfferDto>> sink) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();

        MessageListener listener = message -> {
            if (message != null) {
                processResponse(message, sink);
            } else {
                sink.complete();
            }
        };

        container.setConnectionFactory(rabbitTemplate.getConnectionFactory());
        container.setQueueNames(replyQueue);
        container.setMessageListener(listener);
        container.start();

        container.setErrorHandler(t -> {
            if (t instanceof TimeoutException) {
                logger.error("Timeout error: No response received within the timeout period.");
                sink.complete();
            } else {
                logger.error("Error occurred while receiving the message.", t);
                sink.error(t);
            }
        });

        return container;
    }

    private void processResponse(Message response, FluxSink<List<OfferDto>> sink) {
        try {
            String responseBody = new String(response.getBody(), StandardCharsets.UTF_8);

            List<OfferDto> offers = objectMapper.readValue(responseBody, new TypeReference<List<OfferDto>>() {});
            sink.next(offers);
            sink.complete();

        } catch (Exception e) {
            logger.error("Error processing response", e);
            sink.error(e);
        }
    }
}
