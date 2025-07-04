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
