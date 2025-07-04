package com.ecommerce.microcommerce.controller;

import com.ecommerce.microcommerce.dao.ProductDao;
import com.ecommerce.microcommerce.model.Product;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ProductController {
    
    private final ProductDao productDao;
    
    public ProductController(ProductDao productDao) {
        this.productDao = productDao;
    }
    
    @GetMapping("/produits")
    public List<Product> listeProduits() {
        return productDao.findAll();
    }

    @GetMapping("/produits/{id}")
    public ResponseEntity<?> afficherUnProduit(@PathVariable String id) {
        Product product = productDao.findById(id);
        if (product != null) {
            return ResponseEntity.ok(product);
        } else {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Produit non trouvé");
            error.put("id", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
    
    @PostMapping("/produits")
    public ResponseEntity<?> ajouterProduit(@RequestBody Product product) {
        // Validation
        if (product.getNom() == null || product.getNom().trim().isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Le nom du produit est obligatoire");
            return ResponseEntity.badRequest().body(error);
        }
        
        if (product.getPrix() < 0) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Le prix ne peut pas être négatif");
            return ResponseEntity.badRequest().body(error);
        }
        
        // Réinitialiser l'ID pour forcer la création d'un nouveau produit
        product.setId(null);
        Product savedProduct = productDao.save(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedProduct);
    }
    
    @PutMapping("/produits/{id}")
    public ResponseEntity<?> modifierProduit(@PathVariable String id, @RequestBody Product product) {
        Product existingProduct = productDao.findById(id);
        if (existingProduct == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Produit non trouvé");
            error.put("id", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
        
        // Validation
        if (product.getNom() == null || product.getNom().trim().isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Le nom du produit est obligatoire");
            return ResponseEntity.badRequest().body(error);
        }
        
        if (product.getPrix() < 0) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Le prix ne peut pas être négatif");
            return ResponseEntity.badRequest().body(error);
        }
        
        product.setId(id);
        Product updatedProduct = productDao.save(product);
        return ResponseEntity.ok(updatedProduct);
    }
    
    @DeleteMapping("/produits/{id}")
    public ResponseEntity<Map<String, String>> supprimerProduit(@PathVariable String id) {
        Product existingProduct = productDao.findById(id);
        if (existingProduct != null) {
            productDao.deleteById(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Produit supprimé avec succès");
            response.put("id", id);
            return ResponseEntity.ok(response);
        } else {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Produit non trouvé");
            error.put("id", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
    
    @PatchMapping("/produits/{id}")
    public ResponseEntity<?> mettreAJourPartiellement(@PathVariable String id, @RequestBody Map<String, Object> updates) {
        Product existingProduct = productDao.findById(id);
        if (existingProduct == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Produit non trouvé");
            error.put("id", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
        
        // Mise à jour partielle
        if (updates.containsKey("nom")) {
            existingProduct.setNom((String) updates.get("nom"));
        }
        if (updates.containsKey("prix")) {
            existingProduct.setPrix(((Number) updates.get("prix")).intValue());
        }
        
        Product updatedProduct = productDao.save(existingProduct);
        return ResponseEntity.ok(updatedProduct);
    }
    
    @PostMapping("/produits/bulk")
    public ResponseEntity<Map<String, Object>> ajouterPlusieursProduits(@RequestBody List<Product> products) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Product> savedProducts = products.stream()
                    .map(productDao::save)
                    .toList();
            
            response.put("message", "Produits ajoutés avec succès");
            response.put("count", savedProducts.size());
            response.put("products", savedProducts);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            response.put("error", "Erreur lors de l'ajout des produits");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @DeleteMapping("/produits")
    public ResponseEntity<Map<String, String>> supprimerTousLesProduits() {
        try {
            // Note: Cette méthode supprime TOUS les produits - à utiliser avec précaution!
            List<Product> allProducts = productDao.findAll();
            allProducts.forEach(product -> productDao.deleteById(product.getId()));
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Tous les produits ont été supprimés");
            response.put("count", String.valueOf(allProducts.size()));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Erreur lors de la suppression");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @GetMapping("/produits/count")
    public ResponseEntity<Map<String, Object>> compterProduits() {
        List<Product> products = productDao.findAll();
        Map<String, Object> response = new HashMap<>();
        response.put("count", products.size());
        response.put("message", "Nombre total de produits");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/produits/search")
    public ResponseEntity<List<Product>> rechercherProduits(@RequestParam(required = false) String nom, 
                                                            @RequestParam(required = false) Integer prixMin,
                                                            @RequestParam(required = false) Integer prixMax) {
        List<Product> products = productDao.findAll();
        
        // Filtrage par nom
        if (nom != null && !nom.isEmpty()) {
            products = products.stream()
                    .filter(p -> p.getNom().toLowerCase().contains(nom.toLowerCase()))
                    .toList();
        }
        
        // Filtrage par prix minimum
        if (prixMin != null) {
            products = products.stream()
                    .filter(p -> p.getPrix() >= prixMin)
                    .toList();
        }
        
        // Filtrage par prix maximum
        if (prixMax != null) {
            products = products.stream()
                    .filter(p -> p.getPrix() <= prixMax)
                    .toList();
        }
        
        return ResponseEntity.ok(products);
    }
} 