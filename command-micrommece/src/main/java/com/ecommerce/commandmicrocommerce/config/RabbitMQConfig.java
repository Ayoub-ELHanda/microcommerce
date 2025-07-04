package com.ecommerce.commandmicrocommerce.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchange pour la communication entre services
    public static final String MICROSERVICE_EXCHANGE = "microservice.exchange";

    // Queues pour envoyer des requêtes aux autres services
    public static final String PRODUCT_QUERY_QUEUE = "product.query.queue";
    public static final String CLIENT_QUERY_QUEUE = "client.query.queue";
    
    // Queues pour recevoir des réponses des autres services
    public static final String PRODUCT_RESPONSE_QUEUE = "product.response.queue";
    public static final String CLIENT_RESPONSE_QUEUE = "client.response.queue";

    // ===== NOUVELLES QUEUES POUR LES COMMANDES =====
    
    // Queue pour créer des commandes via RabbitMQ
    public static final String COMMAND_INPUT_QUEUE = "command.input.queue";
    
    // Queue pour les événements de commandes (notifications)
    public static final String COMMAND_EVENTS_QUEUE = "command.events.queue";
    
    // Queue pour les mises à jour de statut
    public static final String COMMAND_STATUS_QUEUE = "command.status.queue";
    
    // Queue pour les réponses des commandes créées
    public static final String COMMAND_RESPONSE_QUEUE = "command.response.queue";

    @Bean
    public DirectExchange microserviceExchange() {
        return new DirectExchange(MICROSERVICE_EXCHANGE);
    }

    // ===== QUEUES EXISTANTES =====
    
    @Bean
    public Queue productQueryQueue() {
        return QueueBuilder.durable(PRODUCT_QUERY_QUEUE).build();
    }

    @Bean
    public Queue clientQueryQueue() {
        return QueueBuilder.durable(CLIENT_QUERY_QUEUE).build();
    }

    @Bean
    public Queue productResponseQueue() {
        return QueueBuilder.durable(PRODUCT_RESPONSE_QUEUE).build();
    }

    @Bean
    public Queue clientResponseQueue() {
        return QueueBuilder.durable(CLIENT_RESPONSE_QUEUE).build();
    }

    // ===== NOUVELLES QUEUES COMMAND =====
    
    @Bean
    public Queue commandInputQueue() {
        return QueueBuilder.durable(COMMAND_INPUT_QUEUE).build();
    }
    
    @Bean
    public Queue commandEventsQueue() {
        return QueueBuilder.durable(COMMAND_EVENTS_QUEUE).build();
    }
    
    @Bean
    public Queue commandStatusQueue() {
        return QueueBuilder.durable(COMMAND_STATUS_QUEUE).build();
    }
    
    @Bean
    public Queue commandResponseQueue() {
        return QueueBuilder.durable(COMMAND_RESPONSE_QUEUE).build();
    }

    // ===== BINDINGS EXISTANTS =====

    @Bean
    public Binding productQueryBinding() {
        return BindingBuilder
                .bind(productQueryQueue())
                .to(microserviceExchange())
                .with("product.query");
    }

    @Bean
    public Binding clientQueryBinding() {
        return BindingBuilder
                .bind(clientQueryQueue())
                .to(microserviceExchange())
                .with("client.query");
    }

    @Bean
    public Binding productResponseBinding() {
        return BindingBuilder
                .bind(productResponseQueue())
                .to(microserviceExchange())
                .with("product.response");
    }

    @Bean
    public Binding clientResponseBinding() {
        return BindingBuilder
                .bind(clientResponseQueue())
                .to(microserviceExchange())
                .with("client.response");
    }

    // ===== NOUVEAUX BINDINGS COMMAND =====
    
    @Bean
    public Binding commandInputBinding() {
        return BindingBuilder
                .bind(commandInputQueue())
                .to(microserviceExchange())
                .with("command.input");
    }
    
    @Bean
    public Binding commandEventsBinding() {
        return BindingBuilder
                .bind(commandEventsQueue())
                .to(microserviceExchange())
                .with("command.events");
    }
    
    @Bean
    public Binding commandStatusBinding() {
        return BindingBuilder
                .bind(commandStatusQueue())
                .to(microserviceExchange())
                .with("command.status");
    }
    
    @Bean
    public Binding commandResponseBinding() {
        return BindingBuilder
                .bind(commandResponseQueue())
                .to(microserviceExchange())
                .with("command.response");
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

    @Bean
    public org.springframework.amqp.rabbit.core.RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new org.springframework.amqp.rabbit.core.RabbitAdmin(connectionFactory);
    }
} 