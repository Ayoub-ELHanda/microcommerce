package com.ecommerce.microcommerce.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "products")
public class Product {
    @Id
    private String id;
    private String nom;
    private int prix;

    // Constructeur par défaut
    public Product() {
    }

    // Constructeur avec tous les paramètres  
    public Product(String id, String nom, int prix) {
        this.id = id;
        this.nom = nom;
        this.prix = prix;
    }

    // Constructeur pour nouveau produit (sans ID)
    public Product(String nom, int prix) {
        this.nom = nom;
        this.prix = prix;
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

    // toString pour le débogage
    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", prix=" + prix +
                '}';
    }
} 