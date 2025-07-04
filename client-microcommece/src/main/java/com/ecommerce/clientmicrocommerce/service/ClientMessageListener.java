package com.ecommerce.clientmicrocommerce.service;

import com.ecommerce.clientmicrocommerce.model.Client;
import com.ecommerce.clientmicrocommerce.repository.ClientRepository;
import com.ecommerce.clientmicrocommerce.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class ClientMessageListener {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = RabbitMQConfig.CLIENT_QUERY_QUEUE)
    public void handleClientQuery(Map<String, Object> message) {
        try {
            String correlationId = (String) message.get("correlationId");
            String clientId = (String) message.get("clientId");
            String action = (String) message.get("action");
            
            System.out.println("👤 Client Service - Requête reçue pour client: " + clientId + " (ID: " + correlationId + ")");

            Map<String, Object> response = new HashMap<>();
            response.put("correlationId", correlationId);
            response.put("service", "client-service");

            // If clientId is provided, treat it as a GET_CLIENT request
            if (clientId != null && !clientId.trim().isEmpty()) {
                handleGetClient(clientId, response);
            } else if ("GET_ALL_CLIENTS".equals(action)) {
                handleGetAllClients(response);
            } else {
                response.put("status", "ERROR");
                response.put("message", "Client ID requis ou action non reconnue");
            }

            // Envoyer la réponse
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.MICROSERVICE_EXCHANGE,
                "client.response",
                response
            );

        } catch (Exception e) {
            System.err.println("❌ Erreur dans Client Service: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleGetClient(String clientId, Map<String, Object> response) {
        try {
            Optional<Client> clientOpt = clientRepository.findById(clientId);
            
            if (clientOpt.isPresent()) {
                Client client = clientOpt.get();
                response.put("status", "SUCCESS");
                response.put("client", Map.of(
                    "id", client.getId(),
                    "nom", client.getNom(),
                    "prenom", client.getPrenom(),
                    "email", client.getEmail(),
                    "telephone", client.getTelephone(),
                    "adresse", client.getAdresse(),
                    "ville", client.getVille(),
                    "codePostal", client.getCodePostal(),
                    "pays", client.getPays()
                ));
                System.out.println("✅ Client trouvé: " + client.getNom() + " " + client.getPrenom());
            } else {
                response.put("status", "NOT_FOUND");
                response.put("message", "Client non trouvé avec l'ID: " + clientId);
                System.out.println("❌ Client non trouvé: " + clientId);
            }
        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Erreur lors de la recherche du client: " + e.getMessage());
        }
    }

    private void handleGetAllClients(Map<String, Object> response) {
        try {
            var clients = clientRepository.findAll();
            response.put("status", "SUCCESS");
            response.put("clients", clients);
            System.out.println("✅ " + clients.size() + " clients retournés");
        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Erreur lors de la récupération des clients: " + e.getMessage());
        }
    }
} 