package com.ecommerce.commandmicrocommerce.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class QueueInitializer implements CommandLineRunner {

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Autowired
    private Queue commandInputQueue;

    @Autowired
    private Queue commandEventsQueue;

    @Autowired
    private Queue commandStatusQueue;

    @Autowired
    private Queue commandResponseQueue;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("🚀 Initialisation des queues Command...");
        
        // Déclarer explicitement toutes les queues Command
        rabbitAdmin.declareQueue(commandInputQueue);
        rabbitAdmin.declareQueue(commandEventsQueue);
        rabbitAdmin.declareQueue(commandStatusQueue);
        rabbitAdmin.declareQueue(commandResponseQueue);
        
        System.out.println("✅ Queues Command créées avec succès !");
        System.out.println("   - " + RabbitMQConfig.COMMAND_INPUT_QUEUE);
        System.out.println("   - " + RabbitMQConfig.COMMAND_EVENTS_QUEUE);
        System.out.println("   - " + RabbitMQConfig.COMMAND_STATUS_QUEUE);
        System.out.println("   - " + RabbitMQConfig.COMMAND_RESPONSE_QUEUE);
    }
} 