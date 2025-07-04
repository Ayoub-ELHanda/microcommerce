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
    
    // ===== NOUVEAU: Store pour les requêtes de mise à jour de stock =====
    private final Map<String, CompletableFuture<Map<String, Object>>> pendingStockRequests = new ConcurrentHashMap<>();

    /**
     * Récupère les informations d'un client
     */
    public CompletableFuture<Map<String, Object>> getClientInfo(String clientId) {
        String correlationId = UUID.randomUUID().toString();
        
        Map<String, Object> message = new HashMap<>();
        message.put("correlationId", correlationId);
        message.put("clientId", clientId);
        message.put("service", "command-service");
        
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        pendingRequests.put(correlationId, future);
        
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.MICROSERVICE_EXCHANGE,
            "client.query",
            message
        );
        
        System.out.println("📤 Requête client envoyée - ID: " + clientId + ", Correlation: " + correlationId);
        
        // Timeout après 10 secondes
        future.orTimeout(10, TimeUnit.SECONDS);
        
        return future;
    }

    /**
     * Récupère les informations d'un produit
     */
    public CompletableFuture<Map<String, Object>> getProductInfo(String productId) {
        String correlationId = UUID.randomUUID().toString();
        
        Map<String, Object> message = new HashMap<>();
        message.put("correlationId", correlationId);
        message.put("productId", productId);
        message.put("service", "command-service");
        
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        pendingRequests.put(correlationId, future);
        
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.MICROSERVICE_EXCHANGE,
            "product.query",
            message
        );
        
        System.out.println("📤 Requête produit envoyée - ID: " + productId + ", Correlation: " + correlationId);
        
        // Timeout après 10 secondes
        future.orTimeout(10, TimeUnit.SECONDS);
        
        return future;
    }

    // ===== NOUVEAU: GESTION DES MISES À JOUR DE STOCK =====

    /**
     * Enregistre une requête de mise à jour de stock
     */
    public void registerStockUpdateRequest(String correlationId, CompletableFuture<Map<String, Object>> future) {
        pendingStockRequests.put(correlationId, future);
        
        // Timeout après 10 secondes
        future.orTimeout(10, TimeUnit.SECONDS);
    }

    /**
     * Envoie un message de mise à jour de stock
     */
    public void sendStockUpdateMessage(Map<String, Object> stockUpdateMessage) {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.MICROSERVICE_EXCHANGE,
            "stock.update",
            stockUpdateMessage
        );
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

    /**
     * ===== NOUVEAU: Écoute les réponses de mise à jour de stock =====
     */
    @RabbitListener(queues = RabbitMQConfig.STOCK_RESPONSE_QUEUE)
    public void handleStockUpdateResponse(Map<String, Object> response) {
        String correlationId = (String) response.get("correlationId");
        String productId = (String) response.get("productId");
        String status = (String) response.get("status");
        
        System.out.println("📥 Réponse mise à jour stock reçue - Produit: " + productId + 
                          ", Status: " + status + 
                          ", Correlation ID: " + correlationId);
        
        CompletableFuture<Map<String, Object>> future = pendingStockRequests.remove(correlationId);
        if (future != null) {
            future.complete(response);
            System.out.println("✅ Réponse mise à jour stock traitée avec succès");
        } else {
            System.out.println("⚠️ Aucune requête de stock en attente pour cette réponse");
        }
    }
} 