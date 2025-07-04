package com.ecommerce.commandmicrocommerce.controller;

import com.ecommerce.commandmicrocommerce.dao.CommandDao;
import com.ecommerce.commandmicrocommerce.model.Command;
import com.ecommerce.commandmicrocommerce.model.CommandItem;
import com.ecommerce.commandmicrocommerce.service.MicroserviceOrchestrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CommandController {
    
    private final CommandDao commandDao;
    
    @Autowired
    private MicroserviceOrchestrator orchestrator;
    
    public CommandController(CommandDao commandDao) {
        this.commandDao = commandDao;
    }
    
    @GetMapping("/commands")
    public List<Command> listeCommands() {
        return commandDao.findAll();
    }

    @GetMapping("/commands/{id}")
    public ResponseEntity<?> afficherUneCommand(@PathVariable String id) {
        Command command = commandDao.findById(id);
        if (command != null) {
            return ResponseEntity.ok(command);
        } else {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Commande non trouvée");
            error.put("id", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
    
    @PostMapping("/commands")
    public ResponseEntity<?> ajouterCommand(@RequestBody Map<String, Object> commandRequest) {
        try {
            System.out.println("🛒 Nouvelle commande reçue...");
            
            // Validation de base
            String clientId = (String) commandRequest.get("clientId");
            if (clientId == null || clientId.trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "L'ID du client est obligatoire");
                return ResponseEntity.badRequest().body(error);
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> itemsRequest = (List<Map<String, Object>>) commandRequest.get("items");
            if (itemsRequest == null || itemsRequest.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "La commande doit contenir au moins un article");
                return ResponseEntity.badRequest().body(error);
            }
            
            // 1. Récupérer les informations du client via RabbitMQ
            System.out.println("👤 Récupération des infos client: " + clientId);
            CompletableFuture<Map<String, Object>> clientFuture = orchestrator.getClientInfo(clientId);
            
            // 2. Récupérer les informations des produits via RabbitMQ
            List<CompletableFuture<Map<String, Object>>> productFutures = new ArrayList<>();
            for (Map<String, Object> itemRequest : itemsRequest) {
                String productId = (String) itemRequest.get("productId");
                if (productId != null) {
                    System.out.println("📦 Récupération des infos produit: " + productId);
                    productFutures.add(orchestrator.getProductInfo(productId));
                }
            }
            
            // 3. Attendre toutes les réponses
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                CompletableFuture.allOf(productFutures.toArray(new CompletableFuture[0])),
                clientFuture
            );
            
            // 4. Traiter les résultats
            return allFutures.thenApply(v -> {
                try {
                    // Récupérer les données du client
                    Map<String, Object> clientResponse = clientFuture.get();
                    String clientStatus = (String) clientResponse.get("status");
                    
                    if (!"SUCCESS".equals(clientStatus)) {
                        Map<String, String> error = new HashMap<>();
                        error.put("error", "Client non trouvé: " + clientId);
                        error.put("details", (String) clientResponse.get("message"));
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
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
                            Map<String, String> error = new HashMap<>();
                            error.put("error", "Produit non trouvé: " + itemRequest.get("productId"));
                            error.put("details", (String) productResponse.get("message"));
                            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
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
                    
                    // ===== VÉRIFICATION ET MISE À JOUR DU STOCK =====
                    
                    // 1. Vérifier d'abord la disponibilité du stock pour tous les produits
                    boolean stockSuffisant = true;
                    String stockErrorMessage = "";
                    
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
                            stockErrorMessage = "Stock insuffisant pour " + productName + 
                                               " (disponible: " + currentStock + ", demandé: " + requestedQuantity + ")";
                            System.out.println("❌ " + stockErrorMessage);
                            break;
                        }
                    }
                    
                    if (!stockSuffisant) {
                        Map<String, String> error = new HashMap<>();
                        error.put("error", "Stock insuffisant");
                        error.put("details", stockErrorMessage);
                        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
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

                    // 4. Attendre les confirmations de mise à jour de stock (optionnel avec timeout court)
                    try {
                        CompletableFuture<Void> allStockUpdates = CompletableFuture.allOf(
                            stockUpdateFutures.toArray(new CompletableFuture[0])
                        );
                        
                        // Attendre maximum 3 secondes pour les mises à jour de stock
                        allStockUpdates.get(3, java.util.concurrent.TimeUnit.SECONDS);
                        
                        System.out.println("✅ Tous les stocks mis à jour avec succès");
                        
                    } catch (java.util.concurrent.TimeoutException e) {
                        System.out.println("⚠️ Timeout lors de la mise à jour du stock - la commande est créée mais le stock pourrait ne pas être à jour");
                    } catch (Exception e) {
                        System.err.println("❌ Erreur lors de la vérification des mises à jour de stock: " + e.getMessage());
                    }
                    
                    // Réponse enrichie
                    Map<String, Object> response = new HashMap<>();
                    response.put("command", savedCommand);
                    response.put("client", clientData);
                    response.put("message", "Commande créée avec succès avec les données des microservices");
                    
                    return ResponseEntity.status(HttpStatus.CREATED).body(response);
                    
                } catch (Exception e) {
                    System.err.println("❌ Erreur lors de la création de la commande: " + e.getMessage());
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "Erreur lors de la création de la commande");
                    error.put("details", e.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
                }
            }).join();
            
        } catch (Exception e) {
            System.err.println("❌ Erreur dans ajouterCommand: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Erreur lors du traitement de la commande");
            error.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @PutMapping("/commands/{id}")
    public ResponseEntity<?> modifierCommand(@PathVariable String id, @RequestBody Command command) {
        Command existingCommand = commandDao.findById(id);
        if (existingCommand == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Commande non trouvée");
            error.put("id", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
        
        command.setId(id);
        Command updatedCommand = commandDao.save(command);
        return ResponseEntity.ok(updatedCommand);
    }
    
    @DeleteMapping("/commands/{id}")
    public ResponseEntity<Map<String, String>> supprimerCommand(@PathVariable String id) {
        Command existingCommand = commandDao.findById(id);
        if (existingCommand != null) {
            commandDao.deleteById(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Commande supprimée avec succès");
            response.put("id", id);
            return ResponseEntity.ok(response);
        } else {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Commande non trouvée");
            error.put("id", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
    
    @PatchMapping("/commands/{id}/status")
    public ResponseEntity<?> changerStatut(@PathVariable String id, @RequestBody Map<String, String> statusUpdate) {
        Command existingCommand = commandDao.findById(id);
        if (existingCommand == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Commande non trouvée");
            error.put("id", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
        
        String newStatus = statusUpdate.get("status");
        if (newStatus == null || newStatus.trim().isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Le statut est obligatoire");
            return ResponseEntity.badRequest().body(error);
        }
        
        existingCommand.setStatus(newStatus);
        Command updatedCommand = commandDao.save(existingCommand);
        return ResponseEntity.ok(updatedCommand);
    }
    
    @GetMapping("/commands/client/{clientId}")
    public ResponseEntity<List<Command>> commandsParClient(@PathVariable String clientId) {
        List<Command> commands = commandDao.findByClientId(clientId);
        return ResponseEntity.ok(commands);
    }
    
    @GetMapping("/commands/status/{status}")
    public ResponseEntity<List<Command>> commandsParStatut(@PathVariable String status) {
        List<Command> commands = commandDao.findByStatus(status);
        return ResponseEntity.ok(commands);
    }
    
    @GetMapping("/commands/count")
    public ResponseEntity<Map<String, Object>> compterCommands() {
        List<Command> commands = commandDao.findAll();
        Map<String, Object> response = new HashMap<>();
        response.put("count", commands.size());
        response.put("message", "Nombre total de commandes");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/commands/statistics")
    public ResponseEntity<Map<String, Object>> statistiques() {
        List<Command> allCommands = commandDao.findAll();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCommands", allCommands.size());
        
        // Compter par statut
        Map<String, Long> statusCount = new HashMap<>();
        allCommands.stream()
                .collect(java.util.stream.Collectors.groupingBy(Command::getStatus, java.util.stream.Collectors.counting()))
                .forEach(statusCount::put);
        stats.put("byStatus", statusCount);
        
        // Calculer le montant total
        double totalAmount = allCommands.stream()
                .mapToDouble(Command::getTotalAmount)
                .sum();
        stats.put("totalAmount", totalAmount);
        
        return ResponseEntity.ok(stats);
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
        
        // Envoyer le message via l'orchestrateur
        orchestrator.sendStockUpdateMessage(stockUpdateMessage);
        
        System.out.println("📤 Mise à jour stock envoyée - Produit: " + productId + 
                          ", Opération: " + operation + 
                          ", Quantité: " + quantity + 
                          ", Correlation: " + correlationId);
        
        return future;
    }
} 