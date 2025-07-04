package com.ecommerce.microcommerce.service;

import com.ecommerce.microcommerce.model.Product;
import com.ecommerce.microcommerce.repository.ProductRepository;
import com.ecommerce.microcommerce.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class ProductMessageListener {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = RabbitMQConfig.PRODUCT_QUERY_QUEUE)
    public void handleProductQuery(Map<String, Object> message) {
        try {
            String correlationId = (String) message.get("correlationId");
            String productId = (String) message.get("productId");
            
            System.out.println("📦 Requête produit reçue - ID: " + productId + ", Correlation: " + correlationId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("correlationId", correlationId);
            response.put("service", "product-service");
            
            if (productId != null) {
                Optional<Product> product = productRepository.findById(productId);
                if (product.isPresent()) {
                    Product foundProduct = product.get();
                    response.put("status", "SUCCESS");
                    response.put("product", foundProduct);
                    response.put("productId", foundProduct.getId());
                    response.put("productName", foundProduct.getNom());
                    response.put("price", foundProduct.getPrix());
                    response.put("stock", foundProduct.getStock());
                    response.put("inStock", foundProduct.isInStock());
                    System.out.println("✅ Produit trouvé: " + foundProduct.getNom());
                } else {
                    response.put("status", "NOT_FOUND");
                    response.put("message", "Produit non trouvé: " + productId);
                    System.out.println("❌ Produit non trouvé: " + productId);
                }
            } else {
                response.put("status", "ERROR");
                response.put("message", "ID produit manquant");
            }
            
            // Envoyer la réponse
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.MICROSERVICE_EXCHANGE,
                "product.response",
                response
            );
            
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du traitement de la requête produit: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ===== NOUVEAU: GESTION DES MISES À JOUR DE STOCK =====

    @RabbitListener(queues = RabbitMQConfig.STOCK_UPDATE_QUEUE)
    public void handleStockUpdate(Map<String, Object> message) {
        try {
            String correlationId = (String) message.get("correlationId");
            String productId = (String) message.get("productId");
            String operation = (String) message.get("operation"); // "REDUCE", "INCREASE", "SET"
            Integer quantity = null;
            
            if (message.get("quantity") != null) {
                quantity = ((Number) message.get("quantity")).intValue();
            }
            
            System.out.println("📦 Mise à jour stock reçue - Produit: " + productId + 
                             ", Opération: " + operation + 
                             ", Quantité: " + quantity + 
                             ", Correlation: " + correlationId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("correlationId", correlationId);
            response.put("service", "product-service");
            response.put("productId", productId);
            response.put("operation", operation);
            
            if (productId == null || operation == null) {
                response.put("status", "ERROR");
                response.put("message", "ProductId et operation sont obligatoires");
                sendStockResponse(response);
                return;
            }
            
            Optional<Product> productOpt = productRepository.findById(productId);
            if (!productOpt.isPresent()) {
                response.put("status", "NOT_FOUND");
                response.put("message", "Produit non trouvé: " + productId);
                sendStockResponse(response);
                return;
            }
            
            Product product = productOpt.get();
            int oldStock = product.getStock();
            
            try {
                switch (operation.toUpperCase()) {
                    case "REDUCE":
                        if (quantity == null || quantity <= 0) {
                            response.put("status", "ERROR");
                            response.put("message", "Quantité invalide pour la réduction");
                            sendStockResponse(response);
                            return;
                        }
                        
                        if (!product.hasEnoughStock(quantity)) {
                            response.put("status", "INSUFFICIENT_STOCK");
                            response.put("message", "Stock insuffisant. Disponible: " + product.getStock() + ", Demandé: " + quantity);
                            response.put("currentStock", product.getStock());
                            response.put("requestedQuantity", quantity);
                            sendStockResponse(response);
                            return;
                        }
                        
                        product.reduceStock(quantity);
                        break;
                        
                    case "INCREASE":
                        if (quantity == null || quantity <= 0) {
                            response.put("status", "ERROR");
                            response.put("message", "Quantité invalide pour l'augmentation");
                            sendStockResponse(response);
                            return;
                        }
                        
                        product.increaseStock(quantity);
                        break;
                        
                    case "SET":
                        if (quantity == null || quantity < 0) {
                            response.put("status", "ERROR");
                            response.put("message", "Quantité invalide pour la définition du stock");
                            sendStockResponse(response);
                            return;
                        }
                        
                        product.setStock(quantity);
                        break;
                        
                    default:
                        response.put("status", "ERROR");
                        response.put("message", "Opération non supportée: " + operation);
                        sendStockResponse(response);
                        return;
                }
                
                // Sauvegarder le produit mis à jour
                Product updatedProduct = productRepository.save(product);
                
                response.put("status", "SUCCESS");
                response.put("message", "Stock mis à jour avec succès");
                response.put("productName", updatedProduct.getNom());
                response.put("oldStock", oldStock);
                response.put("newStock", updatedProduct.getStock());
                response.put("quantity", quantity);
                response.put("product", updatedProduct);
                
                System.out.println("✅ Stock mis à jour: " + updatedProduct.getNom() + 
                                 " (" + oldStock + " → " + updatedProduct.getStock() + ")");
                
            } catch (IllegalArgumentException e) {
                response.put("status", "ERROR");
                response.put("message", e.getMessage());
                response.put("currentStock", product.getStock());
            }
            
            sendStockResponse(response);
            
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la mise à jour du stock: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("correlationId", message.get("correlationId"));
            errorResponse.put("service", "product-service");
            errorResponse.put("status", "ERROR");
            errorResponse.put("message", "Erreur interne: " + e.getMessage());
            sendStockResponse(errorResponse);
        }
    }

    /**
     * Envoie une réponse de mise à jour de stock
     */
    private void sendStockResponse(Map<String, Object> response) {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.MICROSERVICE_EXCHANGE,
            "stock.response",
            response
        );
    }
} 