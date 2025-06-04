package edu.pg.booking.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfiguration {

    @Value("${rabbit.transaction.exchange}")
    private String transactionExchangeName;

    @Value("${rabbit.transaction.rollback.exchange}")
    private String rollbackExchangeName;

    @Value("${rabbit.transaction.queue}")
    private String transactionQueueName;

    @Value("${rabbit.transaction.rollback.queue}")
    private String rollbackQueueName;

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
    public RabbitAdmin rabbitAdmin(
            ConnectionFactory connectionFactory,
            @Qualifier("transactionExchangeName") FanoutExchange transactionExchangeName,
            @Qualifier("transactionQueueName") Queue transactionQueueName,
            @Qualifier("transactionBinding") Binding transactionBinding,
            @Qualifier("rollbackExchangeName") FanoutExchange rollbackExchangeName,
            @Qualifier("rollbackQueueName") Queue rollbackQueueName,
            @Qualifier("rollbackBinding") Binding rollbackBinding
    ) {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitAdmin.declareExchange(transactionExchangeName);
        rabbitAdmin.declareQueue(transactionQueueName);
        rabbitAdmin.declareBinding(transactionBinding);

        rabbitAdmin.declareExchange(rollbackExchangeName);
        rabbitAdmin.declareQueue(rollbackQueueName);
        rabbitAdmin.declareBinding(rollbackBinding);

        return rabbitAdmin;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setReplyTimeout(10000);
        return rabbitTemplate;
    }

    @Bean( name = "transactionExchangeName")
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange(transactionExchangeName);
    }

    @Bean( name = "rollbackExchangeName")
    public FanoutExchange importFanoutExchange() {
        return new FanoutExchange(rollbackExchangeName);
    }

    @Bean(name = "transactionQueueName")
    public Queue serviceQueue() {
        return new Queue(transactionQueueName);
    }

    @Bean(name = "rollbackQueueName")
    public Queue importQueue() {
        return new Queue(rollbackQueueName);
    }

    @Bean(name = "transactionBinding")
    public Binding transactionBinding(
            @Qualifier("transactionExchangeName") FanoutExchange fanoutExchange,
            @Qualifier("transactionQueueName") Queue queue
    ) {
        return BindingBuilder.bind(queue).to(fanoutExchange);
    }

    @Bean(name = "rollbackBinding")
    public Binding rollbackBinding(
            @Qualifier("rollbackExchangeName") FanoutExchange fanoutExchange,
            @Qualifier("rollbackQueueName") Queue queue
    ) {
        return BindingBuilder.bind(queue).to(fanoutExchange);
    }
}
