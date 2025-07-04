package com.ecommerce.commandmicrocommerce.service;

import com.ecommerce.commandmicrocommerce.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class MicroserviceOrchestrator {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // Store pour les réponses asynchrones avec correlation IDs
    private final Map<String, CompletableFuture<Map<String, Object>>> pendingRequests = new ConcurrentHashMap<>();

    /**
     * Récupère les informations d'un client
     */
    public CompletableFuture<Map<String, Object>> getClientInfo(String clientId) {
        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        
        // Stocker la future pour la réponse
        pendingRequests.put(correlationId, future);
        
        // Créer le message de requête
        Map<String, Object> message = new HashMap<>();
        message.put("action", "GET_CLIENT");
        message.put("clientId", clientId);
        message.put("correlationId", correlationId);
        
        System.out.println("🚀 Envoi requête client - ID: " + clientId + " (Correlation: " + correlationId + ")");
        
        // Envoyer la requête
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.MICROSERVICE_EXCHANGE,
            "client.query",
            message
        );
        
        // Timeout après 10 secondes
        CompletableFuture.delayedExecutor(10, TimeUnit.SECONDS).execute(() -> {
            if (!future.isDone()) {
                pendingRequests.remove(correlationId);
                future.completeExceptionally(new RuntimeException("Timeout: Pas de réponse du Client Service"));
            }
        });
        
        return future;
    }

    /**
     * Récupère les informations d'un produit
     */
    public CompletableFuture<Map<String, Object>> getProductInfo(String productId) {
        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        
        // Stocker la future pour la réponse
        pendingRequests.put(correlationId, future);
        
        // Créer le message de requête
        Map<String, Object> message = new HashMap<>();
        message.put("action", "GET_PRODUCT");
        message.put("productId", productId);
        message.put("correlationId", correlationId);
        
        System.out.println("🚀 Envoi requête produit - ID: " + productId + " (Correlation: " + correlationId + ")");
        
        // Envoyer la requête
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.MICROSERVICE_EXCHANGE,
            "product.query",
            message
        );
        
        // Timeout après 10 secondes
        CompletableFuture.delayedExecutor(10, TimeUnit.SECONDS).execute(() -> {
            if (!future.isDone()) {
                pendingRequests.remove(correlationId);
                future.completeExceptionally(new RuntimeException("Timeout: Pas de réponse du Product Service"));
            }
        });
        
        return future;
    }

    /**
     * Écoute les réponses du Client Service
     */
    @RabbitListener(queues = RabbitMQConfig.CLIENT_RESPONSE_QUEUE)
    public void handleClientResponse(Map<String, Object> response) {
        String correlationId = (String) response.get("correlationId");
        System.out.println("📥 Réponse client reçue - Correlation ID: " + correlationId);
        
        CompletableFuture<Map<String, Object>> future = pendingRequests.remove(correlationId);
        if (future != null) {
            future.complete(response);
            System.out.println("✅ Réponse client traitée avec succès");
        } else {
            System.out.println("⚠️ Aucune requête en attente pour cette réponse client");
        }
    }

    /**
     * Écoute les réponses du Product Service
     */
    @RabbitListener(queues = RabbitMQConfig.PRODUCT_RESPONSE_QUEUE)
    public void handleProductResponse(Map<String, Object> response) {
        String correlationId = (String) response.get("correlationId");
        System.out.println("📥 Réponse produit reçue - Correlation ID: " + correlationId);
        
        CompletableFuture<Map<String, Object>> future = pendingRequests.remove(correlationId);
        if (future != null) {
            future.complete(response);
            System.out.println("✅ Réponse produit traitée avec succès");
        } else {
            System.out.println("⚠️ Aucune requête en attente pour cette réponse produit");
        }
    }
} 