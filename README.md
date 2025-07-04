# 🛒 Microcommerce - Système E-commerce avec Microservices

## 🎯 Description

Ce projet implémente une **architecture microservices** pour un système e-commerce avec :
- **3 services indépendants** (Product, Client, Command)
- **Communication asynchrone** via RabbitMQ
- **Bases de données MongoDB séparées**
- **Orchestration intelligente** pour les commandes

## 📋 Description du Projet

**Microcommerce** est une architecture de microservices complète pour un système e-commerce utilisant **Spring Boot**, **MongoDB**, et **RabbitMQ** pour la communication inter-services. Le système est entièrement containerisé avec **Docker** pour un déploiement simplifié.

## 🏗️ Architecture

```
┌─────────────────┐    RabbitMQ     ┌─────────────────┐
│  Product Service│◄──────────────►│ Command Service │
│     Port 8081   │                │   Port 8083     │
│   MongoDB:27017 │                │ MongoDB:27019   │
└─────────────────┘                └─────────────────┘
                                           ▲
                                           │ RabbitMQ
                                           ▼
                                   ┌─────────────────┐
                                   │  Client Service │
                                   │    Port 8082    │
                                   │  MongoDB:27018  │
                                   └─────────────────┘
```

## 🛠️ Technologies Utilisées

- **Backend** : Spring Boot 3.5.3
- **Base de données** : MongoDB
- **Message Broker** : RabbitMQ 3.13.7
- **Containerisation** : Docker & Docker Compose
- **Java** : OpenJDK 17
- **Build Tool** : Maven

## 🚀 Installation et Démarrage

### Prérequis
- Docker Desktop installé et en fonctionnement
- Git (pour cloner le projet)

### 1. Cloner le projet
```bash
git clone
cd microcommerce
```

### 3. Démarrage manuel

**Lancer tous les services :**
```bash
docker-compose -f docker-compose-microservices.yml up -d
```

**Vérifier le statut :**
```bash
docker ps
```

## 🎯 Services et Ports

| Service | Port | API Base | Database Port | Description |
|---------|------|----------|---------------|-------------|
| **Product Service** | 8081 | `/api/produits` | 27017 | Gestion des produits |
| **Client Service** | 8082 | `/api/clients` | 27018 | Gestion des clients |
| **Command Service** | 8083 | `/api/commands` | 27019 | Orchestrateur de commandes |
| **RabbitMQ** | 5672 | Management: 15672 | - | Message broker |

## 🗄️ Interfaces MongoDB (Mongo Express)

| Database | Port | Interface URL | Credentials | Description |
|----------|------|---------------|-------------|-------------|
| **📦 Product Database** | 8091 | http://localhost:8091 | admin/admin123 | Interface web pour la base produits |
| **👥 Client Database** | 8092 | http://localhost:8092 | admin/admin123 | Interface web pour la base clients |
| **🛒 Command Database** | 8093 | http://localhost:8093 | admin/admin123 | Interface web pour la base commandes |

### 🔍 Comment utiliser Mongo Express

1. **Ouvrir l'interface** : Cliquez sur l'URL correspondante
2. **Se connecter** : Utilisez `admin` / `admin123`
3. **Explorer les données** : 
   - Parcourez les collections (tables)
   - Visualisez les documents JSON
   - Recherchez et filtrez les données
   - Modifiez directement les données si nécessaire

## 📚 APIs Disponibles

### 🛍️ Product Service (Port 8081)

#### Endpoints produits
```bash
# Lister tous les produits
GET http://localhost:8081/api/produits

# Obtenir un produit par ID
GET http://localhost:8081/api/produits/{id}

# Créer un nouveau produit
POST http://localhost:8081/api/produits
Content-Type: application/json
{
  "nom": "Nom du produit",
  "prix": 99
}

# Modifier un produit
PUT http://localhost:8081/api/produits/{id}
Content-Type: application/json
{
  "nom": "Nouveau nom",
  "prix": 149
}

# Supprimer un produit
DELETE http://localhost:8081/api/produits/{id}

# Rechercher des produits
GET http://localhost:8081/api/produits/search?nom=smartphone&prixMin=100&prixMax=1000

# Compter les produits
GET http://localhost:8081/api/produits/count

# Ajout en masse
POST http://localhost:8081/api/produits/bulk
Content-Type: application/json
[
  {"nom": "Produit 1", "prix": 100},
  {"nom": "Produit 2", "prix": 200}
]
```

### 👤 Client Service (Port 8082)

#### Endpoints clients
```bash
# Lister tous les clients
GET http://localhost:8082/api/clients

# Obtenir un client par ID
GET http://localhost:8082/api/clients/{id}

# Créer un nouveau client
POST http://localhost:8082/api/clients
Content-Type: application/json
{
  "nom": "Dupont",
  "prenom": "Jean",
  "email": "jean.dupont@email.com",
  "telephone": "0123456789",
  "adresse": "123 Rue de la Paix",
  "ville": "Paris",
  "codePostal": "75001",
  "pays": "France"
}

# Modifier un client
PUT http://localhost:8082/api/clients/{id}

# Supprimer un client
DELETE http://localhost:8082/api/clients/{id}

# Rechercher par email
GET http://localhost:8082/api/clients/search?email=jean@email.com

# Statistiques
GET http://localhost:8082/api/clients/statistics
```

### 🛒 Command Service (Port 8083) - Orchestrateur

#### Endpoints commandes avec RabbitMQ
```bash
# Lister toutes les commandes
GET http://localhost:8083/api/commands

# Obtenir une commande par ID
GET http://localhost:8083/api/commands/{id}

# Créer une commande (avec communication RabbitMQ automatique)
POST http://localhost:8083/api/commands
Content-Type: application/json
{
  "clientId": "CLIENT_ID_HERE",
  "items": [
    {
      "productId": "PRODUCT_ID_HERE",
      "quantity": 2
    }
  ],
  "shippingAddress": "123 Rue de la Livraison, Paris",
  "paymentMethod": "Credit Card",
  "notes": "Livraison rapide SVP"
}

# Modifier le statut d'une commande
PATCH http://localhost:8083/api/commands/{id}/status
Content-Type: application/json
{
  "status": "SHIPPED"
}

# Commandes par client
GET http://localhost:8083/api/commands/client/{clientId}

# Commandes par statut
GET http://localhost:8083/api/commands/status/PENDING

# Statistiques des commandes
GET http://localhost:8083/api/commands/statistics

# Compter les commandes
GET http://localhost:8083/api/commands/count
```

## 🔄 Communication RabbitMQ

### Flow de création de commande

1. **Réception commande** → Command Service (REST API)
2. **Requête client** → RabbitMQ → Client Service
3. **Requête produit** → RabbitMQ → Product Service
4. **Réponses** → RabbitMQ → Command Service
5. **Création commande enrichie** → Sauvegarde MongoDB
6. **Réponse complète** → Client API

### Queues RabbitMQ utilisées

```
product.query.queue     - Requêtes vers Product Service
product.response.queue  - Réponses du Product Service
client.query.queue      - Requêtes vers Client Service
client.response.queue   - Réponses du Client Service
```

### Interface RabbitMQ
- **URL** : http://localhost:15672
- **Login** : admin
- **Mot de passe** : admin123

## 📖 Exemples d'Utilisation Complets

### 1. Créer un produit
```bash
curl -X POST http://localhost:8081/api/produits \
  -H "Content-Type: application/json" \
  -d '{
    "nom": "iPhone 15",
    "prix": 999
  }'
```

### 2. Créer un client
```bash
curl -X POST http://localhost:8082/api/clients \
  -H "Content-Type: application/json" \
  -d '{
    "nom": "Martin",
    "prenom": "Sophie",
    "email": "sophie.martin@email.com",
    "telephone": "0987654321",
    "adresse": "456 Avenue des Champs",
    "ville": "Lyon",
    "codePostal": "69000",
    "pays": "France"
  }'
```

### 3. Créer une commande (Communication RabbitMQ automatique)
```bash
curl -X POST http://localhost:8083/api/commands \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "ID_DU_CLIENT_CRÉÉ",
    "items": [
      {
        "productId": "ID_DU_PRODUIT_CRÉÉ",
        "quantity": 1
      }
    ],
    "shippingAddress": "456 Avenue des Champs, Lyon",
    "paymentMethod": "Credit Card",
    "notes": "Commande urgente"
  }'
```

## 🔧 Commandes Utiles

### Docker Commands
```bash
# Voir le statut de tous les conteneurs
docker-compose -f docker-compose-microservices.yml ps

# Redémarrer un service spécifique
docker-compose -f docker-compose-microservices.yml restart product-service

# Voir les logs d'un service
docker logs product-service --tail 50 -f

# Entrer dans un conteneur
docker exec -it product-service bash

# Reconstruire et redémarrer
docker-compose -f docker-compose-microservices.yml up -d --build

# Nettoyer tout
docker-compose -f docker-compose-microservices.yml down -v
docker system prune -a
```

### MongoDB Commands
```bash
# Se connecter à MongoDB Product
docker exec -it mongodb-product mongosh mongodb://admin:admin123@localhost:27017/product_db

# Se connecter à MongoDB Client
docker exec -it mongodb-client mongosh mongodb://admin:admin123@localhost:27017/client_db

# Se connecter à MongoDB Command
docker exec -it mongodb-command mongosh mongodb://admin:admin123@localhost:27017/command_db
```

### Health Checks
```bash
# Vérifier la santé des services
curl http://localhost:8081/api/produits/count
curl http://localhost:8082/api/clients/count
curl http://localhost:8083/api/commands/count

# Test de connectivité
ping localhost
telnet localhost 8081
telnet localhost 8082
telnet localhost 8083
```

## 📁 Structure du Projet

```
microcommerce/
├── client-microcommece/           # Service de gestion des clients
│   ├── src/main/java/
│   │   └── com/ecommerce/clientmicrocommerce/
│   │       ├── config/RabbitMQConfig.java
│   │       ├── controller/ClientController.java
│   │       ├── dao/ClientDao.java
│   │       ├── model/Client.java
│   │       ├── repository/ClientRepository.java
│   │       └── service/ClientMessageListener.java
│   ├── Dockerfile
│   └── pom.xml
├── command-micrommece/            # Service orchestrateur de commandes
│   ├── src/main/java/
│   │   └── com/ecommerce/commandmicrocommerce/
│   │       ├── config/RabbitMQConfig.java
│   │       ├── controller/CommandController.java
│   │       ├── dao/CommandDao.java
│   │       ├── model/Command.java
│   │       ├── repository/CommandRepository.java
│   │       └── service/MicroserviceOrchestrator.java
│   ├── Dockerfile
│   └── pom.xml
├── product-microcommerce/         # Service de gestion des produits
│   ├── src/main/java/
│   │   └── com/ecommerce/microcommerce/
│   │       ├── config/RabbitMQConfig.java
│   │       ├── controller/ProductController.java
│   │       ├── dao/ProductDao.java
│   │       ├── model/Product.java
│   │       ├── repository/ProductRepository.java
│   │       └── service/ProductMessageListener.java
│   ├── Dockerfile
│   └── pom.xml
├── docker-compose-microservices.yml  # Configuration Docker complète
├── docker-compose-rabbitmq.yml       # RabbitMQ standalone
├── start-microservices.ps1           # Script de démarrage Windows
├── start-microservices.sh            # Script de démarrage Linux/Mac
├── MICROSERVICES_GUIDE.md            # Guide technique détaillé
└── README.md                          # Ce fichier
```

## 🐛 Dépannage

### Problèmes Courants

#### Services "unhealthy"
```bash
# Vérifier les logs
docker logs product-service
docker logs client-service
docker logs command-service

# Redémarrer les services
docker-compose -f docker-compose-microservices.yml restart
```

#### Connexion RabbitMQ échouée
```bash
# Vérifier RabbitMQ
docker logs rabbitmq-microservices

# Redémarrer RabbitMQ
docker-compose -f docker-compose-microservices.yml restart rabbitmq
```

#### MongoDB inaccessible
```bash
# Vérifier les conteneurs MongoDB
docker ps | grep mongodb

# Redémarrer MongoDB
docker-compose -f docker-compose-microservices.yml restart mongodb-product mongodb-client mongodb-command
```

#### Ports occupés
```bash
# Windows - Vérifier les ports utilisés
netstat -ano | findstr :8081
netstat -ano | findstr :8082
netstat -ano | findstr :8083

# Linux/Mac - Vérifier les ports utilisés
lsof -i :8081
lsof -i :8082
lsof -i :8083
```

### Nettoyage Complet
```bash
# Arrêter tous les services
docker-compose -f docker-compose-microservices.yml down -v

# Supprimer les images
docker rmi microcommerce-product-service microcommerce-client-service microcommerce-command-service

# Nettoyer Docker
docker system prune -a --volumes

# Redémarrer tout
docker-compose -f docker-compose-microservices.yml up -d --build
```

## 📊 Monitoring et Surveillance

### RabbitMQ Management
- **URL** : http://localhost:15672
- **Surveillance** : Queues, Exchanges, Connections, Channels
- **Métriques** : Message rates, Memory usage, Disk space

### Logs en Temps Réel
```bash
# Tous les services
docker-compose -f docker-compose-microservices.yml logs -f

# Service spécifique
docker logs product-service -f
docker logs client-service -f
docker logs command-service -f
docker logs rabbitmq-microservices -f
```

### Métriques des Services
```bash
# Statistiques produits
curl http://localhost:8081/api/produits/count

# Statistiques clients  
curl http://localhost:8082/api/clients/statistics

# Statistiques commandes
curl http://localhost:8083/api/commands/statistics
```

## 🎯 Fonctionnalités Clés

✅ **Microservices découplés** avec communication asynchrone
✅ **RabbitMQ** pour l'orchestration inter-services
✅ **MongoDB** avec bases de données séparées par service
✅ **Docker Compose** pour déploiement simplifié
✅ **APIs REST** complètes avec CRUD operations
✅ **Gestion des erreurs** et validation des données
✅ **Correlation IDs** pour tracer les requêtes
✅ **Health checks** automatiques
✅ **Logs structurés** pour debugging
✅ **Scripts de démarrage** automatisés

## 🚀 Évolutions Futures

- [ ] API Gateway avec Spring Cloud Gateway
- [ ] Service Discovery avec Eureka
- [ ] Authentification JWT
- [ ] Métriques avec Prometheus/Grafana
- [ ] Tests d'intégration
- [ ] CI/CD Pipeline
- [ ] Kubernetes deployment
- [ ] Circuit Breaker pattern

## 👥 Support

Pour toute question ou problème :
1. Consultez les logs : `docker-compose logs -f`
2. Vérifiez RabbitMQ : http://localhost:15672
3. Consultez le guide détaillé : `MICROSERVICES_GUIDE.md`

## 📄 Licence

Ce projet est un exemple éducatif pour démontrer une architecture microservices avec Spring Boot et RabbitMQ.

---

**🎉 Votre système de microservices est prêt ! Bon développement !** 

## 📋 API Documentation - CRUD Complet

### 🛍️ **PRODUCT SERVICE** - Port 8081

#### **1. Créer un Produit**
```http
POST http://localhost:8081/produits
Content-Type: application/json

{
  "nom": "iPhone 15",
  "description": "Smartphone Apple dernière génération",
  "prix": 1199.99,
  "quantite": 50
}
```

#### **2. Lister tous les Produits**
```http
GET http://localhost:8081/produits
```

#### **3. Récupérer un Produit par ID**
```http
GET http://localhost:8081/produits/{id}
```

#### **4. Mettre à jour un Produit**
```http
PUT http://localhost:8081/produits/{id}
Content-Type: application/json

{
  "nom": "iPhone 15 Pro",
  "description": "Smartphone Apple Pro avec nouvelles fonctionnalités",
  "prix": 1399.99,
  "quantite": 30
}
```

#### **5. Supprimer un Produit**
```http
DELETE http://localhost:8081/produits/{id}
```

### 👥 **CLIENT SERVICE** - Port 8082

#### **1. Créer un Client**
```http
POST http://localhost:8082/clients
Content-Type: application/json

{
  "nom": "Dupont",
  "prenom": "Jean",
  "email": "jean.dupont@email.com",
  "telephone": "0123456789",
  "adresse": "123 Rue de la Paix, 75001 Paris"
}
```

#### **2. Lister tous les Clients**
```http
GET http://localhost:8082/clients
```

#### **3. Récupérer un Client par ID**
```http
GET http://localhost:8082/clients/{id}
```

#### **4. Mettre à jour un Client**
```http
PUT http://localhost:8082/clients/{id}
Content-Type: application/json

{
  "nom": "Dupont",
  "prenom": "Jean-Michel",
  "email": "jean-michel.dupont@email.com",
  "telephone": "0123456790",
  "adresse": "456 Avenue des Champs, 75008 Paris"
}
```

#### **5. Supprimer un Client**
```http
DELETE http://localhost:8082/clients/{id}
```

### 🛒 **COMMAND SERVICE** - Port 8083

#### **1. Créer une Commande (Orchestration RabbitMQ)**
```http
POST http://localhost:8083/commands
Content-Type: application/json

{
  "clientId": "REMPLACER_PAR_ID_CLIENT_REEL",
  "items": [
    {
      "productId": "REMPLACER_PAR_ID_PRODUIT_REEL",
      "quantity": 2
    },
    {
      "productId": "REMPLACER_PAR_ID_AUTRE_PRODUIT",
      "quantity": 1
    }
  ],
  "shippingAddress": "123 Rue de Livraison, 75001 Paris",
  "paymentMethod": "Carte bancaire",
  "notes": "Livraison urgente SVP"
}
```

#### **2. Lister toutes les Commandes**
```http
GET http://localhost:8083/commands
```

#### **3. Récupérer une Commande par ID**
```http
GET http://localhost:8083/commands/{id}
```

#### **4. Mettre à jour le Statut d'une Commande**
```http
PUT http://localhost:8083/commands/{id}/status
Content-Type: application/json

{
  "status": "SHIPPED"
}
```

**Statuts disponibles :** `PENDING`, `CONFIRMED`, `PROCESSING`, `SHIPPED`, `DELIVERED`, `CANCELLED`

#### **5. Supprimer une Commande**
```http
DELETE http://localhost:8083/commands/{id}
```

## 📦 Collection Postman - Import Rapide

### **Étapes pour Importer dans Postman :**

1. **Ouvrir Postman**
2. **Cliquer sur "Import"** (bouton orange en haut)
3. **Sélectionner "Raw text"**
4. **Coller le JSON ci-dessous**
5. **Cliquer "Continue" puis "Import"**

### **Collection JSON pour Postman :**

```json
{
  "info": {
    "name": "Microcommerce API",
    "description": "Collection complète pour tester tous les microservices",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "variable": [
    {
      "key": "product_base_url",
      "value": "http://localhost:8081"
    },
    {
      "key": "client_base_url", 
      "value": "http://localhost:8082"
    },
    {
      "key": "command_base_url",
      "value": "http://localhost:8083"
    }
  ],
  "item": [
    {
      "name": "🛍️ Product Service",
      "item": [
        {
          "name": "Create Product",
          "request": {
            "method": "POST",
            "header": [
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"nom\": \"Souris Gaming\",\n  \"description\": \"Souris haute précision pour gamers\",\n  \"prix\": 79.99,\n  \"quantite\": 100\n}"
            },
            "url": {
              "raw": "{{product_base_url}}/produits",
              "host": ["{{product_base_url}}"],
              "path": ["produits"]
            }
          }
        },
        {
          "name": "Get All Products",
          "request": {
            "method": "GET",
            "url": {
              "raw": "{{product_base_url}}/produits",
              "host": ["{{product_base_url}}"],
              "path": ["produits"]
            }
          }
        },
        {
          "name": "Get Product by ID",
          "request": {
            "method": "GET", 
            "url": {
              "raw": "{{product_base_url}}/produits/PRODUCT_ID",
              "host": ["{{product_base_url}}"],
              "path": ["produits", "PRODUCT_ID"]
            }
          }
        },
        {
          "name": "Update Product",
          "request": {
            "method": "PUT",
            "header": [
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"nom\": \"Souris Gaming Pro\",\n  \"description\": \"Souris ultra haute précision\",\n  \"prix\": 99.99,\n  \"quantite\": 80\n}"
            },
            "url": {
              "raw": "{{product_base_url}}/produits/PRODUCT_ID",
              "host": ["{{product_base_url}}"],
              "path": ["produits", "PRODUCT_ID"]
            }
          }
        },
        {
          "name": "Delete Product",
          "request": {
            "method": "DELETE",
            "url": {
              "raw": "{{product_base_url}}/produits/PRODUCT_ID",
              "host": ["{{product_base_url}}"],
              "path": ["produits", "PRODUCT_ID"]
            }
          }
        }
      ]
    },
    {
      "name": "👥 Client Service",
      "item": [
        {
          "name": "Create Client",
          "request": {
            "method": "POST",
            "header": [
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"nom\": \"Martin\",\n  \"prenom\": \"Sophie\",\n  \"email\": \"sophie.martin@email.com\",\n  \"telephone\": \"0987654321\",\n  \"adresse\": \"789 Boulevard Saint-Germain, 75006 Paris\"\n}"
            },
            "url": {
              "raw": "{{client_base_url}}/clients",
              "host": ["{{client_base_url}}"],
              "path": ["clients"]
            }
          }
        },
        {
          "name": "Get All Clients",
          "request": {
            "method": "GET",
            "url": {
              "raw": "{{client_base_url}}/clients",
              "host": ["{{client_base_url}}"],
              "path": ["clients"]
            }
          }
        },
        {
          "name": "Get Client by ID",
          "request": {
            "method": "GET",
            "url": {
              "raw": "{{client_base_url}}/clients/CLIENT_ID",
              "host": ["{{client_base_url}}"],
              "path": ["clients", "CLIENT_ID"]
            }
          }
        },
        {
          "name": "Update Client",
          "request": {
            "method": "PUT",
            "header": [
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"nom\": \"Martin\",\n  \"prenom\": \"Sophie-Claire\",\n  \"email\": \"sophie.claire.martin@email.com\",\n  \"telephone\": \"0987654322\",\n  \"adresse\": \"999 Rue de Rivoli, 75001 Paris\"\n}"
            },
            "url": {
              "raw": "{{client_base_url}}/clients/CLIENT_ID",
              "host": ["{{client_base_url}}"],
              "path": ["clients", "CLIENT_ID"]
            }
          }
        },
        {
          "name": "Delete Client",
          "request": {
            "method": "DELETE",
            "url": {
              "raw": "{{client_base_url}}/clients/CLIENT_ID",
              "host": ["{{client_base_url}}"],
              "path": ["clients", "CLIENT_ID"]
            }
          }
        }
      ]
    },
    {
      "name": "🛒 Command Service",
      "item": [
        {
          "name": "Create Command (RabbitMQ Orchestration)",
          "request": {
            "method": "POST",
            "header": [
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"clientId\": \"CLIENT_ID_FROM_PREVIOUS_REQUEST\",\n  \"items\": [\n    {\n      \"productId\": \"PRODUCT_ID_FROM_PREVIOUS_REQUEST\",\n      \"quantity\": 2\n    }\n  ],\n  \"shippingAddress\": \"123 Rue de la Livraison, 75001 Paris\",\n  \"paymentMethod\": \"Carte bancaire\",\n  \"notes\": \"Commande test via Postman\"\n}"
            },
            "url": {
              "raw": "{{command_base_url}}/commands",
              "host": ["{{command_base_url}}"],
              "path": ["commands"]
            }
          }
        },
        {
          "name": "Get All Commands",
          "request": {
            "method": "GET",
            "url": {
              "raw": "{{command_base_url}}/commands",
              "host": ["{{command_base_url}}"],
              "path": ["commands"]
            }
          }
        },
        {
          "name": "Get Command by ID",
          "request": {
            "method": "GET",
            "url": {
              "raw": "{{command_base_url}}/commands/COMMAND_ID",
              "host": ["{{command_base_url}}"],
              "path": ["commands", "COMMAND_ID"]
            }
          }
        },
        {
          "name": "Update Command Status",
          "request": {
            "method": "PUT",
            "header": [
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"status\": \"SHIPPED\"\n}"
            },
            "url": {
              "raw": "{{command_base_url}}/commands/COMMAND_ID/status",
              "host": ["{{command_base_url}}"],
              "path": ["commands", "COMMAND_ID", "status"]
            }
          }
        },
        {
          "name": "Delete Command",
          "request": {
            "method": "DELETE",
            "url": {
              "raw": "{{command_base_url}}/commands/COMMAND_ID",
              "host": ["{{command_base_url}}"],
              "path": ["commands", "COMMAND_ID"]
            }
          }
        }
      ]
    }
  ]
}
```

## 🔄 Workflow de Test Recommandé

### **1. Créer des Données de Test**

1. **Créer un Produit** (Product Service)
2. **Créer un Client** (Client Service)  
3. **Noter les IDs** retournés

### **2. Tester l'Orchestration**

1. **Créer une Commande** avec les IDs récupérés
2. **Vérifier les logs** : `docker logs command-service -f`
3. **Surveiller RabbitMQ** : http://localhost:15672/#/queues

### **3. Vérifier les Communications**

- Les **messages RabbitMQ** transitent instantanément
- Le **Command Service** récupère automatiquement les données
- La **commande enrichie** est sauvegardée avec toutes les informations

## 🐛 Debugging et Logs

```bash
# Logs en temps réel de tous les services
docker-compose -f docker-compose-microservices.yml logs -f

# Logs d'un service spécifique
docker logs product-service -f
docker logs client-service -f  
docker logs command-service -f

# Vérifier l'état de RabbitMQ
docker logs rabbitmq-microservices -f
```

## 🎯 Fonctionnalités Avancées

### **RabbitMQ Queues (8 Total)**

#### **Queues de Communication :**
- `product.query.queue` / `product.response.queue`
- `client.query.queue` / `client.response.queue`

#### **Queues Command Spécifiques :**
- `command.input.queue` - Création de commandes via RabbitMQ
- `command.events.queue` - Événements de commandes  
- `command.status.queue` - Mises à jour de statut
- `command.response.queue` - Réponses des commandes

### **Orchestration Intelligente**

Le **Command Service** :
- ✅ Reçoit une demande de commande
- ✅ Interroge le **Client Service** via RabbitMQ
- ✅ Interroge le **Product Service** via RabbitMQ  
- ✅ Crée une **commande enrichie** avec toutes les données
- ✅ Sauvegarde dans sa propre base MongoDB
- ✅ Retourne la **réponse complète**

## 🚀 Architecture Technique

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  Product Service│    │  Client Service │    │ Command Service │
│     :8081       │    │     :8082       │    │     :8083       │
│   MongoDB:27017 │    │   MongoDB:27018 │    │   MongoDB:27019 │
└─────────┬───────┘    └─────────┬───────┘    └─────────┬───────┘
          │                      │                      │
          └──────────────────────┼──────────────────────┘
                                 │
                    ┌─────────────┴───────────┐
                    │      RabbitMQ           │
                    │    :5672 / :15672       │
                    │  8 Queues + Exchange    │
                    └─────────────────────────┘
```

**🎉 Votre système microservices est maintenant complet et prêt pour la production !** 