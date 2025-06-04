package edu.pg.query.config;

import jakarta.annotation.PostConstruct;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class RabbitConfiguration {

    @Value("${rabbit.rpc.exchange}")
    private String exchangeName;

    @Value("${rabbit.rpc.response.exchange}")
    private String responseExchangeName;

    @Value("${rabbit.transaction.exchange}")
    private String transactionExchangeName;

    @Value("${rabbit.transaction.query.queue}")
    private String transactionQueueName;

    @Value("${rabbit.host}")
    private String host;

    @Value("${rabbit.port}")
    private Integer port;

    @Value("${rabbit.username}")
    private String username;

    @Value("${rabbit.password}")
    private String password;

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory(host);
        factory.setPort(port);
        factory.setUsername(username);
        factory.setPassword(password);
        return factory;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setReplyTimeout(60000);
        return rabbitTemplate;
    }

    @Primary
    @Bean(name = "rpcExchange")
    public FanoutExchange rpcExchange() {
        return new FanoutExchange(exchangeName);
    }

    @Bean(name = "transactionExchange")
    public FanoutExchange transactionExchange() {
        return new FanoutExchange(transactionExchangeName);
    }

    @Bean(name = "transactionQueue")
    public Queue transactionQueue() {
        return new Queue(transactionQueueName);
    }

    @Bean(name = "transactionBinding")
    public Binding transactionBinding(
            @Qualifier("transactionExchange") FanoutExchange fanoutExchange,
            @Qualifier("transactionQueue") Queue queue
    ) {
        return BindingBuilder.bind(queue).to(fanoutExchange);
    }

    @Bean
    public DirectExchange rpcResponseExchange() {
        return new DirectExchange(responseExchangeName);
    }

    @Bean
    public RabbitAdmin rabbitAdmin(
            ConnectionFactory connectionFactory,
            @Qualifier("rpcExchange") FanoutExchange fanoutExchange,
            DirectExchange rpcResponseExchange,
            @Qualifier("transactionExchange") FanoutExchange transactionExchange,
            @Qualifier("transactionQueue") Queue transactionQueue,
            @Qualifier("transactionBinding") Binding transactionBinding
    ) {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitAdmin.declareExchange(fanoutExchange);
        rabbitAdmin.declareExchange(rpcResponseExchange);

        rabbitAdmin.declareExchange(transactionExchange);
        rabbitAdmin.declareQueue(transactionQueue);
        rabbitAdmin.declareBinding(transactionBinding);

        return rabbitAdmin;
    }
}
