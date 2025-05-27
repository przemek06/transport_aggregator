package edu.pg.to.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfiguration {

    @Value("${rabbit.rpc.exchange}")
    private String exchangeName;

    @Value("${rabbit.rpc.response.exchange}")
    private String responseExchangeName;

    @Value("${rabbit.rpc.queue}")
    private String queueName;

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
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory, FanoutExchange fanoutExchange, DirectExchange rpcResponseExchange, Queue serviceQueue, Binding binding) {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitAdmin.declareExchange(fanoutExchange);
        rabbitAdmin.declareExchange(rpcResponseExchange);
        rabbitAdmin.declareQueue(serviceQueue);
        rabbitAdmin.declareBinding(binding);
        return rabbitAdmin;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setReplyTimeout(10000);
        return rabbitTemplate;
    }

    @Bean
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange(exchangeName);
    }

    @Bean
    public Queue serviceQueue() {
        return new Queue(queueName);
    }

    @Bean
    public Binding bindingServiceQueue(FanoutExchange fanoutExchange, Queue serviceQueue) {
        return BindingBuilder.bind(serviceQueue).to(fanoutExchange);
    }

    @Bean
    public DirectExchange rpcResponseExchange() {
        return new DirectExchange(responseExchangeName);
    }
}
