package com.ecommerce.clientmicrocommerce.dao;

import com.ecommerce.clientmicrocommerce.model.Client;
import com.ecommerce.clientmicrocommerce.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.util.List;

@Repository
public class ClientDaoImpl implements ClientDao {
    
    @Autowired
    private ClientRepository clientRepository;
    
    // Initialisation des données de test dans MongoDB
    @PostConstruct
    public void initData() {
        // Vérifier si la base de données est vide
        if (clientRepository.count() == 0) {
            // Ajouter des données de test
            clientRepository.save(new Client("Dupont", "Jean", "jean.dupont@email.com", "0123456789", "123 Rue de la Paix", "Paris", "75001", "France"));
            clientRepository.save(new Client("Martin", "Marie", "marie.martin@email.com", "0123456790", "456 Avenue des Champs", "Lyon", "69001", "France"));
            clientRepository.save(new Client("Bernard", "Pierre", "pierre.bernard@email.com", "0123456791", "789 Boulevard Voltaire", "Marseille", "13001", "France"));
            clientRepository.save(new Client("Durand", "Sophie", "sophie.durand@email.com", "0123456792", "321 Rue de Rivoli", "Toulouse", "31000", "France"));
            clientRepository.save(new Client("Moreau", "Paul", "paul.moreau@email.com", "0123456793", "654 Avenue Montaigne", "Nice", "06000", "France"));
            clientRepository.save(new Client("Simon", "Claire", "claire.simon@email.com", "0123456794", "987 Rue Saint-Honoré", "Strasbourg", "67000", "France"));
            clientRepository.save(new Client("Michel", "Antoine", "antoine.michel@email.com", "0123456795", "147 Boulevard Haussmann", "Bordeaux", "33000", "France"));
            clientRepository.save(new Client("Garcia", "Elena", "elena.garcia@email.com", "0123456796", "258 Rue Lafayette", "Lille", "59000", "France"));
            clientRepository.save(new Client("Rodriguez", "Carlos", "carlos.rodriguez@email.com", "0123456797", "369 Avenue Foch", "Nantes", "44000", "France"));
        }
    }
    
    @Override
    public List<Client> findAll() {
        return clientRepository.findAll();
    }
    
    @Override
    public Client findById(String id) {
        return clientRepository.findById(id).orElse(null);
    }
    
    @Override
    public Client save(Client client) {
        return clientRepository.save(client);
    }
    
    @Override
    public void deleteById(String id) {
        clientRepository.deleteById(id);
    }
    
    @Override
    public Client findByEmail(String email) {
        return clientRepository.findByEmail(email).orElse(null);
    }
    
    @Override
    public List<Client> findByNom(String nom) {
        return clientRepository.findByNomContainingIgnoreCase(nom);
    }
    
    @Override
    public boolean existsByEmail(String email) {
        return clientRepository.existsByEmail(email);
    }
} 