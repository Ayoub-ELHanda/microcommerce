package com.ecommerce.microcommerce.dao;

import com.ecommerce.microcommerce.model.Product;
import com.ecommerce.microcommerce.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.util.List;

@Repository
public class ProductDaoImpl implements ProductDao {
    
    @Autowired
    private ProductRepository productRepository;
    
    // Initialisation des données de test dans MongoDB
    @PostConstruct
    public void initData() {
        // Vérifier si la base de données est vide
        if (productRepository.count() == 0) {
            // Ajouter des données de test
            productRepository.save(new Product("Ordinateur portable", 800));
            productRepository.save(new Product("Souris sans fil", 25));
            productRepository.save(new Product("Clavier mécanique", 120));
            productRepository.save(new Product("Écran 24 pouces", 250));
            productRepository.save(new Product("Casque audio", 80));
            productRepository.save(new Product("Webcam HD", 60));
            productRepository.save(new Product("Tapis de souris", 15));
            productRepository.save(new Product("Disque dur externe", 90));
            productRepository.save(new Product("Chargeur USB-C", 30));
        }
    }
    
    @Override
    public List<Product> findAll() {
        return productRepository.findAll();
    }
    
    @Override
    public Product findById(String id) {
        return productRepository.findById(id).orElse(null);
    }
    
    @Override
    public Product save(Product product) {
        return productRepository.save(product);
    }
    
    @Override
    public void deleteById(String id) {
        productRepository.deleteById(id);
    }
} 