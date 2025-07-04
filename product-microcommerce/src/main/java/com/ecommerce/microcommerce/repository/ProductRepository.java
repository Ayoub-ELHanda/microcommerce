package com.ecommerce.microcommerce.repository;

import com.ecommerce.microcommerce.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    
    // MongoRepository already provides:
    // - findAll()
    // - findById(String id)
    // - save(Product product)
    // - deleteById(String id)
    
    // We can add custom queries here if needed
    Optional<Product> findByNom(String nom);
} 