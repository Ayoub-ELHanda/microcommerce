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

                // Sauvegarder
                Command savedCommand = commandDao.save(command);

                response.put("status", "SUCCESS");
                response.put("command", savedCommand);
                response.put("client", clientData);
                response.put("message", "Commande créée avec succès via RabbitMQ");

                // Publier un événement de création
                publishCommandEvent("COMMAND_CREATED", savedCommand, "Nouvelle commande créée via RabbitMQ");

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
} 