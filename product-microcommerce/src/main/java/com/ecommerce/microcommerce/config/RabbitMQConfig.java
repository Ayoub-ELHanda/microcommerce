package com.ecommerce.microcommerce.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Queue pour les requêtes de produits
    public static final String PRODUCT_QUERY_QUEUE = "product.query.queue";
    public static final String PRODUCT_RESPONSE_QUEUE = "product.response.queue";
    
    // ===== NOUVELLES QUEUES POUR LA GESTION DU STOCK =====
    public static final String STOCK_UPDATE_QUEUE = "stock.update.queue";
    public static final String STOCK_RESPONSE_QUEUE = "stock.response.queue";
    
    // Exchange pour la communication entre services
    public static final String MICROSERVICE_EXCHANGE = "microservice.exchange";

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
    public Queue productResponseQueue() {
        return QueueBuilder.durable(PRODUCT_RESPONSE_QUEUE).build();
    }

    // ===== NOUVELLES QUEUES STOCK =====

    @Bean
    public Queue stockUpdateQueue() {
        return QueueBuilder.durable(STOCK_UPDATE_QUEUE).build();
    }

    @Bean
    public Queue stockResponseQueue() {
        return QueueBuilder.durable(STOCK_RESPONSE_QUEUE).build();
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
    public Binding productResponseBinding() {
        return BindingBuilder
                .bind(productResponseQueue())
                .to(microserviceExchange())
                .with("product.response");
    }

    // ===== NOUVEAUX BINDINGS STOCK =====

    @Bean
    public Binding stockUpdateBinding() {
        return BindingBuilder
                .bind(stockUpdateQueue())
                .to(microserviceExchange())
                .with("stock.update");
    }

    @Bean
    public Binding stockResponseBinding() {
        return BindingBuilder
                .bind(stockResponseQueue())
                .to(microserviceExchange())
                .with("stock.response");
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