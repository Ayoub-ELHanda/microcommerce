package com.ecommerce.clientmicrocommerce.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Queue pour les requêtes de clients
    public static final String CLIENT_QUERY_QUEUE = "client.query.queue";
    public static final String CLIENT_RESPONSE_QUEUE = "client.response.queue";
    
    // Exchange pour la communication entre services
    public static final String MICROSERVICE_EXCHANGE = "microservice.exchange";

    @Bean
    public DirectExchange microserviceExchange() {
        return new DirectExchange(MICROSERVICE_EXCHANGE);
    }

    @Bean
    public Queue clientQueryQueue() {
        return QueueBuilder.durable(CLIENT_QUERY_QUEUE).build();
    }

    @Bean
    public Queue clientResponseQueue() {
        return QueueBuilder.durable(CLIENT_RESPONSE_QUEUE).build();
    }

    @Bean
    public Binding clientQueryBinding() {
        return BindingBuilder
                .bind(clientQueryQueue())
                .to(microserviceExchange())
                .with("client.query");
    }

    @Bean
    public Binding clientResponseBinding() {
        return BindingBuilder
                .bind(clientResponseQueue())
                .to(microserviceExchange())
                .with("client.response");
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
} 