package com.ecommerce.clientmicrocommerce.controller;

import com.ecommerce.clientmicrocommerce.dao.ClientDao;
import com.ecommerce.clientmicrocommerce.model.Client;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ClientController {
    
    private final ClientDao clientDao;
    
    public ClientController(ClientDao clientDao) {
        this.clientDao = clientDao;
    }
    
    @GetMapping("/clients")
    public List<Client> listeClients() {
        return clientDao.findAll();
    }

    @GetMapping("/clients/{id}")
    public ResponseEntity<?> afficherUnClient(@PathVariable String id) {
        Client client = clientDao.findById(id);
        if (client != null) {
            return ResponseEntity.ok(client);
        } else {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Client non trouvé");
            error.put("id", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
    
    @PostMapping("/clients")
    public ResponseEntity<?> ajouterClient(@RequestBody Client client) {
        // Validation
        if (client.getNom() == null || client.getNom().trim().isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Le nom du client est obligatoire");
            return ResponseEntity.badRequest().body(error);
        }
        
        if (client.getEmail() == null || client.getEmail().trim().isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "L'email du client est obligatoire");
            return ResponseEntity.badRequest().body(error);
        }
        
        if (clientDao.existsByEmail(client.getEmail())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Un client avec cet email existe déjà");
            return ResponseEntity.badRequest().body(error);
        }
        
        // Réinitialiser l'ID pour forcer la création d'un nouveau client
        client.setId(null);
        Client savedClient = clientDao.save(client);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedClient);
    }
    
    @PutMapping("/clients/{id}")
    public ResponseEntity<?> modifierClient(@PathVariable String id, @RequestBody Client client) {
        Client existingClient = clientDao.findById(id);
        if (existingClient == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Client non trouvé");
            error.put("id", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
        
        // Validation
        if (client.getNom() == null || client.getNom().trim().isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Le nom du client est obligatoire");
            return ResponseEntity.badRequest().body(error);
        }
        
        if (client.getEmail() == null || client.getEmail().trim().isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "L'email du client est obligatoire");
            return ResponseEntity.badRequest().body(error);
        }
        
        // Vérifier si l'email existe déjà pour un autre client
        Client clientWithEmail = clientDao.findByEmail(client.getEmail());
        if (clientWithEmail != null && !clientWithEmail.getId().equals(id)) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Un autre client avec cet email existe déjà");
            return ResponseEntity.badRequest().body(error);
        }
        
        client.setId(id);
        Client updatedClient = clientDao.save(client);
        return ResponseEntity.ok(updatedClient);
    }
    
    @DeleteMapping("/clients/{id}")
    public ResponseEntity<Map<String, String>> supprimerClient(@PathVariable String id) {
        Client existingClient = clientDao.findById(id);
        if (existingClient != null) {
            clientDao.deleteById(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Client supprimé avec succès");
            response.put("id", id);
            return ResponseEntity.ok(response);
        } else {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Client non trouvé");
            error.put("id", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
    
    @PatchMapping("/clients/{id}")
    public ResponseEntity<?> mettreAJourPartiellement(@PathVariable String id, @RequestBody Map<String, Object> updates) {
        Client existingClient = clientDao.findById(id);
        if (existingClient == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Client non trouvé");
            error.put("id", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
        
        // Mise à jour partielle
        if (updates.containsKey("nom")) {
            existingClient.setNom((String) updates.get("nom"));
        }
        if (updates.containsKey("prenom")) {
            existingClient.setPrenom((String) updates.get("prenom"));
        }
        if (updates.containsKey("email")) {
            String newEmail = (String) updates.get("email");
            if (clientDao.existsByEmail(newEmail) && !existingClient.getEmail().equals(newEmail)) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Un client avec cet email existe déjà");
                return ResponseEntity.badRequest().body(error);
            }
            existingClient.setEmail(newEmail);
        }
        if (updates.containsKey("telephone")) {
            existingClient.setTelephone((String) updates.get("telephone"));
        }
        if (updates.containsKey("adresse")) {
            existingClient.setAdresse((String) updates.get("adresse"));
        }
        if (updates.containsKey("ville")) {
            existingClient.setVille((String) updates.get("ville"));
        }
        if (updates.containsKey("codePostal")) {
            existingClient.setCodePostal((String) updates.get("codePostal"));
        }
        if (updates.containsKey("pays")) {
            existingClient.setPays((String) updates.get("pays"));
        }
        
        Client updatedClient = clientDao.save(existingClient);
        return ResponseEntity.ok(updatedClient);
    }
    
    @PostMapping("/clients/bulk")
    public ResponseEntity<Map<String, Object>> ajouterPlusieursClients(@RequestBody List<Client> clients) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Client> savedClients = clients.stream()
                    .map(client -> {
                        client.setId(null); // Force creation
                        return clientDao.save(client);
                    })
                    .toList();
            
            response.put("message", "Clients ajoutés avec succès");
            response.put("count", savedClients.size());
            response.put("clients", savedClients);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            response.put("error", "Erreur lors de l'ajout des clients");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @DeleteMapping("/clients")
    public ResponseEntity<Map<String, String>> supprimerTousLesClients() {
        try {
            List<Client> allClients = clientDao.findAll();
            allClients.forEach(client -> clientDao.deleteById(client.getId()));
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Tous les clients ont été supprimés");
            response.put("count", String.valueOf(allClients.size()));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Erreur lors de la suppression");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @GetMapping("/clients/count")
    public ResponseEntity<Map<String, Object>> compterClients() {
        List<Client> clients = clientDao.findAll();
        Map<String, Object> response = new HashMap<>();
        response.put("count", clients.size());
        response.put("message", "Nombre total de clients");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/clients/search")
    public ResponseEntity<List<Client>> rechercherClients(@RequestParam(required = false) String nom, 
                                                          @RequestParam(required = false) String email,
                                                          @RequestParam(required = false) String ville,
                                                          @RequestParam(required = false) String pays) {
        List<Client> clients = clientDao.findAll();
        
        // Filtrage par nom
        if (nom != null && !nom.isEmpty()) {
            clients = clients.stream()
                    .filter(c -> c.getNom().toLowerCase().contains(nom.toLowerCase()) || 
                               c.getPrenom().toLowerCase().contains(nom.toLowerCase()))
                    .toList();
        }
        
        // Filtrage par email
        if (email != null && !email.isEmpty()) {
            clients = clients.stream()
                    .filter(c -> c.getEmail().toLowerCase().contains(email.toLowerCase()))
                    .toList();
        }
        
        // Filtrage par ville
        if (ville != null && !ville.isEmpty()) {
            clients = clients.stream()
                    .filter(c -> c.getVille().toLowerCase().contains(ville.toLowerCase()))
                    .toList();
        }
        
        // Filtrage par pays
        if (pays != null && !pays.isEmpty()) {
            clients = clients.stream()
                    .filter(c -> c.getPays().toLowerCase().contains(pays.toLowerCase()))
                    .toList();
        }
        
        return ResponseEntity.ok(clients);
    }
    
    @GetMapping("/clients/email/{email}")
    public ResponseEntity<?> rechercherParEmail(@PathVariable String email) {
        Client client = clientDao.findByEmail(email);
        if (client != null) {
            return ResponseEntity.ok(client);
        } else {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Aucun client trouvé avec cet email");
            error.put("email", email);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
} 