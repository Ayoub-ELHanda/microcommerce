package com.ecommerce.clientmicrocommerce.dao;

import com.ecommerce.clientmicrocommerce.model.Client;

import java.util.List;

public interface ClientDao {
    
    List<Client> findAll();
    
    Client findById(String id);
    
    Client save(Client client);
    
    void deleteById(String id);
    
    // Custom methods
    Client findByEmail(String email);
    
    List<Client> findByNom(String nom);
    
    boolean existsByEmail(String email);
} 