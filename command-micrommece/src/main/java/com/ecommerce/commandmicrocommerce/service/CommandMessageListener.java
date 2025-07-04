package com.ecommerce.commandmicrocommerce.service;

import com.ecommerce.commandmicrocommerce.config.RabbitMQConfig;
import com.ecommerce.commandmicrocommerce.dao.CommandDao;
import com.ecommerce.commandmicrocommerce.model.Command;
import com.ecommerce.commandmicrocommerce.model.CommandItem;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

@Service
public class CommandMessageListener {

    @Autowired
    private CommandDao commandDao;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private MicroserviceOrchestrator orchestrator;

    /**
     * Écoute les demandes de création de commandes via RabbitMQ
     */
    @RabbitListener(queues = RabbitMQConfig.COMMAND_INPUT_QUEUE)
    public void handleCommandCreation(Map<String, Object> commandRequest) {
        try {
            String correlationId = (String) commandRequest.get("correlationId");
            System.out.println("🛒 Commande reçue via RabbitMQ - ID: " + correlationId);

            Map<String, Object> response = new HashMap<>();
            response.put("correlationId", correlationId);
            response.put("service", "command-service");

            // Validation de base
            String clientId = (String) commandRequest.get("clientId");
            if (clientId == null || clientId.trim().isEmpty()) {
                response.put("status", "ERROR");
                response.put("message", "L'ID du client est obligatoire");
                sendCommandResponse(response);
                return;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> itemsRequest = (List<Map<String, Object>>) commandRequest.get("items");
            if (itemsRequest == null || itemsRequest.isEmpty()) {
                response.put("status", "ERROR");
                response.put("message", "La commande doit contenir au moins un article");
                sendCommandResponse(response);
                return;
            }

            // Traitement asynchrone de la commande
            processCommandAsync(commandRequest, response);

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du traitement de la commande RabbitMQ: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Écoute les demandes de mise à jour de statut via RabbitMQ
     */
    @RabbitListener(queues = RabbitMQConfig.COMMAND_STATUS_QUEUE)
    public void handleStatusUpdate(Map<String, Object> statusRequest) {
        try {
            String correlationId = (String) statusRequest.get("correlationId");
            String commandId = (String) statusRequest.get("commandId");
            String newStatus = (String) statusRequest.get("status");
            
            System.out.println("📊 Mise à jour statut reçue - Command: " + commandId + ", Status: " + newStatus);

            Map<String, Object> response = new HashMap<>();
            response.put("correlationId", correlationId);
            response.put("service", "command-service");

            if (commandId == null || newStatus == null) {
                response.put("status", "ERROR");
                response.put("message", "ID de commande et statut requis");
                sendCommandResponse(response);
                return;
            }

            // Mettre à jour le statut
            Command existingCommand = commandDao.findById(commandId);
            if (existingCommand != null) {
                existingCommand.setStatus(newStatus);
                Command updatedCommand = commandDao.save(existingCommand);
                
                response.put("status", "SUCCESS");
                response.put("command", updatedCommand);
                response.put("message", "Statut mis à jour avec succès");

                // Publier un événement de changement de statut
                publishCommandEvent("STATUS_UPDATED", updatedCommand, "Statut changé à " + newStatus);
                
                System.out.println("✅ Statut mis à jour: " + commandId + " → " + newStatus);
            } else {
                response.put("status", "NOT_FOUND");
                response.put("message", "Commande non trouvée: " + commandId);
            }

            sendCommandResponse(response);

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la mise à jour du statut: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Traite la création de commande de manière asynchrone
     */
    private void processCommandAsync(Map<String, Object> commandRequest, Map<String, Object> response) {
        String clientId = (String) commandRequest.get("clientId");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> itemsRequest = (List<Map<String, Object>>) commandRequest.get("items");

        // 1. Récupérer les informations du client via RabbitMQ
        CompletableFuture<Map<String, Object>> clientFuture = orchestrator.getClientInfo(clientId);

        // 2. Récupérer les informations des produits via RabbitMQ
        List<CompletableFuture<Map<String, Object>>> productFutures = new ArrayList<>();
        for (Map<String, Object> itemRequest : itemsRequest) {
            String productId = (String) itemRequest.get("productId");
            if (productId != null) {
                productFutures.add(orchestrator.getProductInfo(productId));
            }
        }

        // 3. Attendre toutes les réponses et traiter
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            CompletableFuture.allOf(productFutures.toArray(new CompletableFuture[0])),
            clientFuture
        );

        allFutures.thenApply(v -> {
            try {
                // Récupérer les données du client
                Map<String, Object> clientResponse = clientFuture.get();
                String clientStatus = (String) clientResponse.get("status");

                if (!"SUCCESS".equals(clientStatus)) {
                    response.put("status", "ERROR");
                    response.put("message", "Client non trouvé: " + clientId);
                    sendCommandResponse(response);
                    return null;
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> clientData = (Map<String, Object>) clientResponse.get("client");

                // Créer les items avec les données des produits
                List<CommandItem> commandItems = new ArrayList<>();
                for (int i = 0; i < itemsRequest.size(); i++) {
                    Map<String, Object> itemRequest = itemsRequest.get(i);
                    Map<String, Object> productResponse = productFutures.get(i).get();

                    String productStatus = (String) productResponse.get("status");
                    if (!"SUCCESS".equals(productStatus)) {
                        response.put("status", "ERROR");
                        response.put("message", "Produit non trouvé: " + itemRequest.get("productId"));
                        sendCommandResponse(response);
                        return null;
                    }

                    @SuppressWarnings("unchecked")
                    Map<String, Object> productData = (Map<String, Object>) productResponse.get("product");

                    // Créer l'item de commande
                    CommandItem item = new CommandItem();
                    item.setProductId((String) productData.get("id"));
                    item.setProductName((String) productData.get("nom"));
                    item.setQuantity((Integer) itemRequest.get("quantity"));
                    item.setUnitPrice(((Number) productData.get("prix")).doubleValue());

                    commandItems.add(item);
                }

                // Créer la commande
                Command command = new Command();
                command.setClientId(clientId);
                command.setClientName((String) clientData.get("nom") + " " + (String) clientData.get("prenom"));
                command.setClientEmail((String) clientData.get("email"));
                command.setItems(commandItems);
                command.setShippingAddress((String) commandRequest.get("shippingAddress"));
                command.setPaymentMethod((String) commandRequest.get("paymentMethod"));
                command.setNotes((String) commandRequest.get("notes"));

                // ===== NOUVEAU: VÉRIFICATION ET MISE À JOUR DU STOCK =====
                
                // 1. Vérifier d'abord la disponibilité du stock pour tous les produits
                boolean stockSuffisant = true;
                
                for (int i = 0; i < itemsRequest.size(); i++) {
                    Map<String, Object> itemRequest = itemsRequest.get(i);
                    Map<String, Object> productResponse = productFutures.get(i).get();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> productData = (Map<String, Object>) productResponse.get("product");
                    
                    String productName = (String) productData.get("nom");
                    int currentStock = ((Number) productData.get("stock")).intValue();
                    int requestedQuantity = (Integer) itemRequest.get("quantity");
                    
                    if (currentStock < requestedQuantity) {
                        stockSuffisant = false;
                        System.out.println("❌ Commande rejetée - Stock insuffisant: " + productName + 
                                           ": Stock insuffisant (disponible: " + currentStock + ", demandé: " + requestedQuantity + ").");
                    }
                }
                
                if (!stockSuffisant) {
                    response.put("status", "INSUFFICIENT_STOCK");
                    response.put("message", "Stock insuffisant pour certains produits");
                    sendCommandResponse(response);
                    return null;
                }

                // 2. Si le stock est suffisant, sauvegarder la commande D'ABORD
                Command savedCommand = commandDao.save(command);
                System.out.println("✅ Commande sauvegardée: " + savedCommand.getId());

                // 3. Ensuite, réduire le stock via RabbitMQ pour chaque produit
                List<CompletableFuture<Map<String, Object>>> stockUpdateFutures = new ArrayList<>();
                
                for (CommandItem item : commandItems) {
                    CompletableFuture<Map<String, Object>> stockUpdateFuture = sendStockUpdate(
                        item.getProductId(), 
                        "REDUCE", 
                        item.getQuantity(),
                        savedCommand.getId()
                    );
                    stockUpdateFutures.add(stockUpdateFuture);
                    
                    System.out.println("📦 Mise à jour stock envoyée - Produit: " + item.getProductName() + 
                                     ", Quantité: -" + item.getQuantity());
                }

                // 4. Attendre les confirmations de mise à jour de stock (optionnel)
                try {
                    CompletableFuture<Void> allStockUpdates = CompletableFuture.allOf(
                        stockUpdateFutures.toArray(new CompletableFuture[0])
                    );
                    
                    // Attendre maximum 5 secondes pour les mises à jour de stock
                    allStockUpdates.get(5, java.util.concurrent.TimeUnit.SECONDS);
                    
                    // Vérifier les résultats des mises à jour
                    boolean stockUpdateSuccess = true;
                    
                    for (int i = 0; i < stockUpdateFutures.size(); i++) {
                        Map<String, Object> stockResponse = stockUpdateFutures.get(i).get();
                        String stockStatus = (String) stockResponse.get("status");
                        
                        if (!"SUCCESS".equals(stockStatus)) {
                            stockUpdateSuccess = false;
                            CommandItem item = commandItems.get(i);
                            System.out.println("⚠️ Problèmes lors de la mise à jour du stock: " + item.getProductName() + 
                                               ": " + stockResponse.get("message"));
                        }
                    }
                    
                    if (!stockUpdateSuccess) {
                        // Marquer la commande comme ayant des problèmes de stock
                        savedCommand.setStatus("STOCK_ERROR");
                        savedCommand.setNotes("ERREUR STOCK: Stock insuffisant pour certains produits");
                        commandDao.save(savedCommand);
                        
                        System.out.println("⚠️ Problèmes lors de la mise à jour du stock");
                    } else {
                        System.out.println("✅ Tous les stocks mis à jour avec succès");
                    }
                    
                } catch (java.util.concurrent.TimeoutException e) {
                    System.out.println("⚠️ Timeout lors de la mise à jour du stock - la commande est créée mais le stock pourrait ne pas être à jour");
                } catch (Exception e) {
                    System.err.println("❌ Erreur lors de la vérification des mises à jour de stock: " + e.getMessage());
                }

                response.put("status", "SUCCESS");
                response.put("command", savedCommand);
                response.put("client", clientData);
                response.put("message", "Commande créée avec succès via RabbitMQ - Stock mis à jour");

                // Publier un événement de création
                publishCommandEvent("COMMAND_CREATED", savedCommand, "Nouvelle commande créée via RabbitMQ avec mise à jour du stock");

                sendCommandResponse(response);

                System.out.println("✅ Commande créée via RabbitMQ: " + savedCommand.getId());

                return null;

            } catch (Exception e) {
                System.err.println("❌ Erreur lors de la création de la commande: " + e.getMessage());
                response.put("status", "ERROR");
                response.put("message", "Erreur lors de la création: " + e.getMessage());
                sendCommandResponse(response);
                return null;
            }
        });
    }

    /**
     * Envoie une réponse pour les commandes
     */
    private void sendCommandResponse(Map<String, Object> response) {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.MICROSERVICE_EXCHANGE,
            "command.response",
            response
        );
    }

    /**
     * Publie un événement de commande
     */
    private void publishCommandEvent(String eventType, Command command, String description) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", eventType);
            event.put("commandId", command.getId());
            event.put("clientId", command.getClientId());
            event.put("status", command.getStatus());
            event.put("totalAmount", command.getTotalAmount());
            event.put("description", description);
            event.put("timestamp", System.currentTimeMillis());

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.MICROSERVICE_EXCHANGE,
                "command.events",
                event
            );

            System.out.println("📢 Événement publié: " + eventType + " pour commande " + command.getId());

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la publication de l'événement: " + e.getMessage());
        }
    }

    /**
     * Envoie une mise à jour de stock via RabbitMQ
     */
    private CompletableFuture<Map<String, Object>> sendStockUpdate(String productId, String operation, int quantity, String commandId) {
        String correlationId = java.util.UUID.randomUUID().toString();
        
        Map<String, Object> stockUpdateMessage = new HashMap<>();
        stockUpdateMessage.put("correlationId", correlationId);
        stockUpdateMessage.put("productId", productId);
        stockUpdateMessage.put("operation", operation);
        stockUpdateMessage.put("quantity", quantity);
        stockUpdateMessage.put("commandId", commandId);
        stockUpdateMessage.put("service", "command-service");
        stockUpdateMessage.put("timestamp", System.currentTimeMillis());
        
        // Créer un CompletableFuture pour la réponse
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        orchestrator.registerStockUpdateRequest(correlationId, future);
        
        // Envoyer le message
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.MICROSERVICE_EXCHANGE,
            "stock.update",
            stockUpdateMessage
        );
        
        System.out.println("📤 Mise à jour stock envoyée - Produit: " + productId + 
                          ", Opération: " + operation + 
                          ", Quantité: " + quantity + 
                          ", Correlation: " + correlationId);
        
        return future;
    }
} 