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
            String action = (String) message.get("action");
            String correlationId = (String) message.get("correlationId");
            
            System.out.println("📦 Product Service - Requête reçue: " + action + " (ID: " + correlationId + ")");

            Map<String, Object> response = new HashMap<>();
            response.put("correlationId", correlationId);
            response.put("service", "product-service");

            switch (action) {
                case "GET_PRODUCT":
                    String productId = (String) message.get("productId");
                    handleGetProduct(productId, response);
                    break;
                
                case "GET_ALL_PRODUCTS":
                    handleGetAllProducts(response);
                    break;
                
                default:
                    response.put("status", "ERROR");
                    response.put("message", "Action non reconnue: " + action);
            }

            // Envoyer la réponse
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.MICROSERVICE_EXCHANGE,
                "product.response",
                response
            );

        } catch (Exception e) {
            System.err.println("❌ Erreur dans Product Service: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleGetProduct(String productId, Map<String, Object> response) {
        try {
            Optional<Product> productOpt = productRepository.findById(productId);
            
            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                response.put("status", "SUCCESS");
                response.put("product", Map.of(
                    "id", product.getId(),
                    "nom", product.getNom(),
                    "prix", product.getPrix()
                ));
                System.out.println("✅ Produit trouvé: " + product.getNom());
            } else {
                response.put("status", "NOT_FOUND");
                response.put("message", "Produit non trouvé avec l'ID: " + productId);
                System.out.println("❌ Produit non trouvé: " + productId);
            }
        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Erreur lors de la recherche du produit: " + e.getMessage());
        }
    }

    private void handleGetAllProducts(Map<String, Object> response) {
        try {
            var products = productRepository.findAll();
            response.put("status", "SUCCESS");
            response.put("products", products);
            System.out.println("✅ " + products.size() + " produits retournés");
        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Erreur lors de la récupération des produits: " + e.getMessage());
        }
    }
} 