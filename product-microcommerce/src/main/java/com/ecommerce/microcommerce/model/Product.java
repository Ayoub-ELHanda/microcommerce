package com.ecommerce.microcommerce.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "products")
public class Product {
    @Id
    private String id;
    private String nom;
    private int prix;
    private int stock; // Gestion du stock

    // Constructeur par défaut
    public Product() {
    }

    // Constructeur avec tous les paramètres  
    public Product(String id, String nom, int prix, int stock) {
        this.id = id;
        this.nom = nom;
        this.prix = prix;
        this.stock = stock;
    }

    // Constructeur pour nouveau produit (sans ID)
    public Product(String nom, int prix, int stock) {
        this.nom = nom;
        this.prix = prix;
        this.stock = stock;
    }

    // Constructeur pour compatibilité (sans stock)
    public Product(String nom, int prix) {
        this.nom = nom;
        this.prix = prix;
        this.stock = 0; // Stock par défaut à 0
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public int getPrix() {
        return prix;
    }

    public int getStock() {
        return stock;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setPrix(int prix) {
        this.prix = prix;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    // Méthodes utiles pour la gestion du stock
    public boolean isInStock() {
        return stock > 0;
    }

    public boolean hasEnoughStock(int quantity) {
        return stock >= quantity;
    }

    public void reduceStock(int quantity) {
        if (quantity > stock) {
            throw new IllegalArgumentException("Stock insuffisant. Stock disponible: " + stock + ", quantité demandée: " + quantity);
        }
        this.stock -= quantity;
    }

    public void increaseStock(int quantity) {
        this.stock += quantity;
    }

    // toString pour le débogage
    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", prix=" + prix +
                ", stock=" + stock +
                '}';
    }
} 