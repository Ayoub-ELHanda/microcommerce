# Command Service - CRUD Operations

Documentation complète des opérations CRUD pour le service de gestion des commandes.

## Base URL
```
http://localhost:8083/api
```

## Endpoints Disponibles

### 📋 Liste des Endpoints

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/commands` | Récupérer toutes les commandes |
| GET | `/commands/{id}` | Récupérer une commande par ID |
| POST | `/commands` | Créer une nouvelle commande |
| PUT | `/commands/{id}` | Modifier une commande complète |
| PATCH | `/commands/{id}/status` | Modifier le statut d'une commande |
| DELETE | `/commands/{id}` | Supprimer une commande |
| GET | `/commands/client/{clientId}` | Commandes par client |
| GET | `/commands/status/{status}` | Commandes par statut |
| GET | `/commands/count` | Compter les commandes |
| GET | `/commands/statistics` | Statistiques des commandes |

---

## 🔍 READ Operations

### 1. Récupérer toutes les commandes

```http
GET /commands
```

**Réponse:**
```json
[
  {
    "id": "6867caf164ff991ccb3dbd6f",
    "clientId": "68678f98ba1a43846781d929",
    "clientName": "Dupont Jean",
    "clientEmail": "jean.dupont@email.com",
    "items": [
      {
        "productId": "6867affef596063526aff95f",
        "productName": "Test Product",
        "quantity": 2,
        "unitPrice": 1500.0,
        "totalPrice": 3000.0
      }
    ],
    "totalAmount": 3000.0,
    "status": "PENDING",
    "orderDate": "2025-07-04T12:37:05.655",
    "deliveryDate": null,
    "shippingAddress": "123 Rue Test",
    "paymentMethod": "Credit Card",
    "notes": "Test final"
  }
]
```

### 2. Récupérer une commande par ID

```http
GET /commands/{id}
```

**Exemple:**
```bash
curl http://localhost:8083/api/commands/6867caf164ff991ccb3dbd6f
```

**Réponse succès:**
```json
{
  "id": "6867caf164ff991ccb3dbd6f",
  "clientId": "68678f98ba1a43846781d929",
  "clientName": "Dupont Jean",
  "clientEmail": "jean.dupont@email.com",
  "items": [
    {
      "productId": "6867affef596063526aff95f",
      "productName": "Test Product",
      "quantity": 2,
      "unitPrice": 1500.0,
      "totalPrice": 3000.0
    }
  ],
  "totalAmount": 3000.0,
  "status": "PENDING",
  "orderDate": "2025-07-04T12:37:05.655",
  "shippingAddress": "123 Rue Test",
  "paymentMethod": "Credit Card",
  "notes": "Test final"
}
```

**Réponse erreur (404):**
```json
{
  "error": "Commande non trouvée",
  "id": "invalid-id"
}
```

### 3. Commandes par client

```http
GET /commands/client/{clientId}
```

**Exemple:**
```bash
curl http://localhost:8083/api/commands/client/68678f98ba1a43846781d929
```

### 4. Commandes par statut

```http
GET /commands/status/{status}
```

**Statuts disponibles:** PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED

**Exemple:**
```bash
curl http://localhost:8083/api/commands/status/PENDING
```

### 5. Compter les commandes

```http
GET /commands/count
```

**Réponse:**
```json
{
  "count": 15,
  "message": "Nombre total de commandes"
}
```

### 6. Statistiques des commandes

```http
GET /commands/statistics
```

**Réponse:**
```json
{
  "totalCommands": 15,
  "byStatus": {
    "PENDING": 8,
    "DELIVERED": 5,
    "CANCELLED": 2
  },
  "totalAmount": 45000.0
}
```

---

## ➕ CREATE Operation

### Créer une nouvelle commande

```http
POST /commands
Content-Type: application/json
```

**Corps de la requête:**
```json
{
  "clientId": "68678f98ba1a43846781d929",
  "items": [
    {
      "productId": "6867affef596063526aff95f",
      "quantity": 2
    },
    {
      "productId": "another-product-id",
      "quantity": 1
    }
  ],
  "shippingAddress": "123 Rue de la Livraison, Paris 75001",
  "paymentMethod": "Credit Card",
  "notes": "Livraison rapide SVP"
}
```

**Champs obligatoires:**
- `clientId` (string)
- `items` (array, minimum 1 élément)
  - `productId` (string)
  - `quantity` (integer, > 0)
- `shippingAddress` (string)
- `paymentMethod` (string)

**Champs optionnels:**
- `notes` (string)

**Exemple avec curl:**
```bash
curl -X POST http://localhost:8083/api/commands \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "68678f98ba1a43846781d929",
    "items": [
      {
        "productId": "6867affef596063526aff95f",
        "quantity": 2
      }
    ],
    "shippingAddress": "123 Rue Test",
    "paymentMethod": "Credit Card",
    "notes": "Commande test"
  }'
```

**Réponse succès (201):**
```json
{
  "command": {
    "id": "new-command-id",
    "clientId": "68678f98ba1a43846781d929",
    "clientName": "Dupont Jean",
    "clientEmail": "jean.dupont@email.com",
    "items": [
      {
        "productId": "6867affef596063526aff95f",
        "productName": "Test Product",
        "quantity": 2,
        "unitPrice": 1500.0,
        "totalPrice": 3000.0
      }
    ],
    "totalAmount": 3000.0,
    "status": "PENDING",
    "orderDate": "2025-07-04T14:30:00.000",
    "shippingAddress": "123 Rue Test",
    "paymentMethod": "Credit Card",
    "notes": "Commande test"
  },
  "client": {
    "nom": "Dupont",
    "prenom": "Jean",
    "email": "jean.dupont@email.com"
  },
  "message": "Commande créée avec succès avec les données des microservices"
}
```

**Erreurs possibles:**

**400 - Client ID manquant:**
```json
{
  "error": "L'ID du client est obligatoire"
}
```

**400 - Items manquants:**
```json
{
  "error": "La commande doit contenir au moins un article"
}
```

**404 - Client non trouvé:**
```json
{
  "error": "Client non trouvé: client-id",
  "details": "Aucun client avec cet ID"
}
```

**404 - Produit non trouvé:**
```json
{
  "error": "Produit non trouvé: product-id",
  "details": "Aucun produit avec cet ID"
}
```

**409 - Stock insuffisant:**
```json
{
  "error": "Stock insuffisant",
  "details": "Stock insuffisant pour Test Product (disponible: 1, demandé: 5)"
}
```

---

## ✏️ UPDATE Operations

### 1. Modifier une commande complète

```http
PUT /commands/{id}
Content-Type: application/json
```

**Corps de la requête:**
```json
{
  "clientId": "68678f98ba1a43846781d929",
  "clientName": "Dupont Jean",
  "clientEmail": "jean.dupont@email.com",
  "items": [
    {
      "productId": "6867affef596063526aff95f",
      "productName": "Test Product",
      "quantity": 3,
      "unitPrice": 1500.0,
      "totalPrice": 4500.0
    }
  ],
  "totalAmount": 4500.0,
  "status": "PROCESSING",
  "shippingAddress": "456 Nouvelle Adresse",
  "paymentMethod": "PayPal",
  "notes": "Commande modifiée"
}
```

**Exemple:**
```bash
curl -X PUT http://localhost:8083/api/commands/6867caf164ff991ccb3dbd6f \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "68678f98ba1a43846781d929",
    "status": "PROCESSING",
    "shippingAddress": "Nouvelle adresse",
    "paymentMethod": "PayPal"
  }'
```

**Réponse succès:**
```json
{
  "id": "6867caf164ff991ccb3dbd6f",
  "clientId": "68678f98ba1a43846781d929",
  "status": "PROCESSING",
  "shippingAddress": "Nouvelle adresse",
  "paymentMethod": "PayPal",
  "orderDate": "2025-07-04T12:37:05.655"
}
```

### 2. Modifier le statut uniquement

```http
PATCH /commands/{id}/status
Content-Type: application/json
```

**Corps de la requête:**
```json
{
  "status": "SHIPPED"
}
```

**Statuts valides:**
- `PENDING` - En attente
- `PROCESSING` - En cours de traitement
- `SHIPPED` - Expédiée
- `DELIVERED` - Livrée
- `CANCELLED` - Annulée

**Exemple:**
```bash
curl -X PATCH http://localhost:8083/api/commands/6867caf164ff991ccb3dbd6f/status \
  -H "Content-Type: application/json" \
  -d '{"status": "SHIPPED"}'
```

**Réponse succès:**
```json
{
  "id": "6867caf164ff991ccb3dbd6f",
  "clientId": "68678f98ba1a43846781d929",
  "clientName": "Dupont Jean",
  "status": "SHIPPED",
  "orderDate": "2025-07-04T12:37:05.655",
  "shippingAddress": "123 Rue Test",
  "paymentMethod": "Credit Card"
}
```

**Erreurs:**

**400 - Statut manquant:**
```json
{
  "error": "Le statut est obligatoire"
}
```

**404 - Commande non trouvée:**
```json
{
  "error": "Commande non trouvée",
  "id": "invalid-id"
}
```

---

## 🗑️ DELETE Operation

### Supprimer une commande

```http
DELETE /commands/{id}
```

**Exemple:**
```bash
curl -X DELETE http://localhost:8083/api/commands/6867caf164ff991ccb3dbd6f
```

**Réponse succès (200):**
```json
{
  "message": "Commande supprimée avec succès",
  "id": "6867caf164ff991ccb3dbd6f"
}
```

**Réponse erreur (404):**
```json
{
  "error": "Commande non trouvée",
  "id": "invalid-id"
}
```

---

## 🧪 Tests avec PowerShell

### Créer une commande
```powershell
$body = @{
    clientId = "68678f98ba1a43846781d929"
    items = @(
        @{
            productId = "6867affef596063526aff95f"
            quantity = 2
        }
    )
    shippingAddress = "123 Rue Test"
    paymentMethod = "Credit Card"
    notes = "Test PowerShell"
} | ConvertTo-Json -Depth 3

Invoke-RestMethod -Uri "http://localhost:8083/api/commands" -Method POST -Body $body -ContentType "application/json"
```

### Récupérer toutes les commandes
```powershell
Invoke-RestMethod -Uri "http://localhost:8083/api/commands" -Method GET
```

### Modifier le statut
```powershell
$statusUpdate = @{ status = "SHIPPED" } | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8083/api/commands/{id}/status" -Method PATCH -Body $statusUpdate -ContentType "application/json"
```

### Supprimer une commande
```powershell
Invoke-RestMethod -Uri "http://localhost:8083/api/commands/{id}" -Method DELETE
```

---

## 📝 Notes Importantes

### Gestion Automatique du Stock
- Lors de la création d'une commande (POST), le stock est automatiquement vérifié et réduit
- Si le stock est insuffisant, la commande est refusée avec une erreur 409
- La mise à jour du stock se fait via RabbitMQ de manière asynchrone

### Intégration avec d'autres Services
- **Client Service**: Récupération automatique des informations client
- **Product Service**: Récupération des détails produits et gestion du stock
- **Communication**: Via RabbitMQ avec gestion des timeouts

### Validation des Données
- `clientId` doit exister dans le service Client
- `productId` doit exister dans le service Product
- `quantity` doit être un nombre positif
- Les adresses et méthodes de paiement sont validées

### Performance
- Requêtes asynchrones vers les autres services
- Timeout de 10 secondes pour les requêtes inter-services
- Gestion des erreurs et retry automatique

### Logs de Débogage
```bash
# Voir les logs du service
docker logs command-service --tail 50

# Logs en temps réel
docker logs -f command-service
```

---

## 🔧 Structure de Réponse Standard

Toutes les réponses suivent ces formats :

**Succès:**
```json
{
  "data": { ... },
  "message": "Operation successful"
}
```

**Erreur:**
```json
{
  "error": "Description de l'erreur",
  "details": "Détails supplémentaires (optionnel)",
  "code": "ERROR_CODE (optionnel)"
}
```

**Codes d'état HTTP:**
- `200` - Succès (GET, PUT, PATCH, DELETE)
- `201` - Créé (POST)
- `400` - Requête invalide
- `404` - Ressource non trouvée
- `409` - Conflit (ex: stock insuffisant)
- `500` - Erreur serveur 