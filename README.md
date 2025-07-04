# Microcommerce - Système de Gestion de Stock

Système de microservices e-commerce avec gestion automatique des stocks.

## Architecture

Le projet comprend 3 microservices :

- **Product Service** (Port 8081) - Gestion des produits et stocks
- **Client Service** (Port 8082) - Gestion des clients  
- **Command Service** (Port 8083) - Gestion des commandes

## Technologies

- Java 17 + Spring Boot 3.5.3
- MongoDB pour la base de données
- RabbitMQ pour la communication entre services
- Docker et Docker Compose

## Fonctionnalité Stock

### Comment ça marche

Quand une commande est créée :

1. Le système vérifie le stock disponible
2. Si insuffisant, la commande est refusée
3. Si suffisant, la commande est créée et le stock est automatiquement réduit
4. La mise à jour se fait via RabbitMQ entre les services

### Exemple

```
Stock initial : 25
- Commande de 3 unités → Stock devient 22
- Commande de 1 unité → Stock devient 21  
- Commande de 25 unités → ERREUR (stock insuffisant)
```

## Installation

### Démarrer les services

```bash
docker-compose -f docker-compose-microservices.yml up -d --build
```

### Vérifier le statut

```bash
docker-compose -f docker-compose-microservices.yml ps
```

### Accès aux services

- Product API: http://localhost:8081/api/produits
- Client API: http://localhost:8082/api/clients
- Command API: http://localhost:8083/api/commands
- RabbitMQ Management: http://localhost:15672 (admin/admin)
- MongoDB Express Product: http://localhost:8091
- MongoDB Express Client: http://localhost:8092  
- MongoDB Express Command: http://localhost:8093

## API Usage

### Créer une commande

```http
POST http://localhost:8083/api/commands
Content-Type: application/json

{
  "clientId": "68678f98ba1a43846781d929",
  "items": [
    {
      "productId": "6867affef596063526aff95f",
      "quantity": 2
    }
  ],
  "shippingAddress": "123 Rue de la Livraison, Paris",
  "paymentMethod": "Credit Card",
  "notes": "Livraison rapide"
}
```

### Réponse succès

```json
{
  "command": {
    "id": "6867bbe364ff991ccb3dbd6e",
    "clientName": "Dupont Jean",
    "items": [
      {
        "productName": "Test Product",
        "quantity": 2,
        "unitPrice": 1500.0,
        "totalPrice": 3000.0
      }
    ],
    "totalAmount": 3000.0,
    "status": "PENDING"
  },
  "message": "Commande créée avec succès"
}
```

### Réponse erreur stock

```json
{
  "error": "Stock insuffisant",
  "details": "Stock insuffisant pour Test Product (disponible: 1, demandé: 5)"
}
```

### Vérifier un produit

```http
GET http://localhost:8081/api/produits/{id}
```

Réponse :
```json
{
  "id": "6867affef596063526aff95f",
  "nom": "Test Product",
  "prix": 1500,
  "stock": 21,
  "inStock": true
}
```

## Détails techniques

### Code de mise à jour du stock

Le stock est mis à jour dans plusieurs endroits :

**1. CommandController (Command Service)**
- Ligne 180-192 : Envoi du message RabbitMQ
- Ligne 342-368 : Méthode `sendStockUpdate()`

**2. ProductMessageListener (Product Service)** 
- Ligne 100-130 : Traitement du message RabbitMQ
- Ligne 150-165 : Sauvegarde en base

**3. Product Model**
- Ligne 75 : `this.stock -= quantity;` (réduction effective)
- Ligne 80 : `this.stock += quantity;` (augmentation)

### Message RabbitMQ

```json
{
  "correlationId": "uuid",
  "productId": "productId", 
  "operation": "REDUCE",
  "quantity": 2,
  "commandId": "commandId"
}
```

### Operations supportées

- `REDUCE` : Diminuer le stock
- `INCREASE` : Augmenter le stock
- `SET` : Définir une valeur exacte

## Tests

### Test création commande

```bash
curl -X POST http://localhost:8083/api/commands \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "68678f98ba1a43846781d929",
    "items": [{"productId": "6867affef596063526aff95f", "quantity": 2}],
    "shippingAddress": "123 Rue Test",
    "paymentMethod": "Credit Card"
  }'
```

### Test vérification stock

```bash
curl http://localhost:8081/api/produits/6867affef596063526aff95f
```

## Logs utiles

### Command Service
```
Nouvelle commande reçue...
Commande sauvegardée: 6867bbe364ff991ccb3dbd6e
Mise à jour stock envoyée - Produit: Test Product, Quantité: -2
Tous les stocks mis à jour avec succès
```

### Product Service  
```
Mise à jour stock reçue - Produit: 6867affef596063526aff95f, Opération: REDUCE
Stock mis à jour: Test Product (25 → 23)
```

## Problèmes courants

### Timeout lors création commande

**Cause** : Services pas complètement démarrés ou RabbitMQ non disponible

**Solution** : 
```bash
# Vérifier les logs
docker logs command-service
docker logs product-service  
docker logs rabbitmq-microservices

# Redémarrer si nécessaire
docker-compose -f docker-compose-microservices.yml restart
```

### Stock non mis à jour

**Vérifications** :
1. RabbitMQ fonctionne-t-il ? `docker logs rabbitmq-microservices`
2. Product Service écoute-t-il les messages ? `docker logs product-service`
3. Queues créées ? Vérifier http://localhost:15672

## Améliorations possibles

- Réservation temporaire de stock
- Historique des mouvements
- Notifications de rupture de stock
- Interface d'administration
- Tests automatisés

## Structure du projet

```
microcommerce/
├── client-microcommece/          # Service clients
├── command-micrommece/           # Service commandes  
├── product-microcommerce/        # Service produits
└── docker-compose-microservices.yml
```