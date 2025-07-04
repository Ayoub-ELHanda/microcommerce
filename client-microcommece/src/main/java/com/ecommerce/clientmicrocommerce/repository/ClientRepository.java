package com.ecommerce.clientmicrocommerce.repository;

import com.ecommerce.clientmicrocommerce.model.Client;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends MongoRepository<Client, String> {
    
    // MongoRepository already provides:
    // - findAll()
    // - findById(String id)
    // - save(Client client)
    // - deleteById(String id)
    
    // Custom queries
    Optional<Client> findByEmail(String email);
    List<Client> findByNomContainingIgnoreCase(String nom);
    List<Client> findByVille(String ville);
    List<Client> findByPays(String pays);
    boolean existsByEmail(String email);
} 