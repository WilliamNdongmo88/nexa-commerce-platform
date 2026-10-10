# Architecture — Microservices Platform

> Document de référence. Toute décision technique doit être cohérente avec ce document.
> Version : 1.0.0 — Dernière mise à jour : initiale

---

## 1. Vue d'ensemble

Plateforme e-commerce composée de **7 microservices métier** et **3 modules d'infrastructure**, avec communication synchrone (WebClient) et asynchrone (Kafka), sécurité JWT, et saga orchestrée pour les transactions distribuées.

### 1.1 Vue logique

```
                        ┌─────────────────────┐
                        │   Client (Web/Mob)  │
                        └──────────┬──────────┘
                                   │ HTTPS
                                   ▼
                        ┌─────────────────────┐
                        │    API Gateway      │ :8080
                        │  (Spring Gateway)   │
                        │  + JWT validation   │
                        └──────────┬──────────┘
                                   │
        ┌──────────────┬───────────┼───────────┬──────────────┐
        ▼              ▼           ▼           ▼              ▼
   ┌────────┐    ┌────────┐  ┌─────────┐ ┌──────────┐  ┌─────────────┐
   │  auth  │    │  user  │  │ product │ │inventory │  │   order     │
   │ :8081  │    │ :8082  │  │ :8083   │ │ :8084    │  │  :8085      │
   └───┬────┘    └───┬────┘  └────┬────┘ └────┬─────┘  └──────┬──────┘
       │             │            │           │               │
       ▼             ▼            ▼           ▼               ▼
   [auth_db]    [user_db]   [product_db] [inventory_db]   [order_db]


   ┌────────────┐        ┌──────────────────┐
   │  payment   │        │  notification    │
   │  :8086     │        │  :8087           │
   └─────┬──────┘        └────────┬─────────┘
         ▼                        │
    [payment_db]                  │ (consumer Kafka uniquement)
                                  │
                        ┌─────────┴──────────┐
                        │      KAFKA         │
                        │  :9092 (broker)    │
                        └────────────────────┘

   ┌──────────────┐    ┌────────────────┐
   │   Eureka     │    │ Config Server  │
   │   :8761      │    │    :8888       │
   └──────────────┘    └────────────────┘
```

### 1.2 Vue physique (production cible)

- **Kubernetes** : 1 namespace `microservices-prod`
- **Base de données** : PostgreSQL managé (ou opérateur CloudNativePG)
- **Kafka** : cluster managé (Strimzi ou Confluent Cloud)
- **Ingress** : NGINX Ingress Controller (terminaison TLS)
- **Secrets** : Kubernetes Secrets + Sealed Secrets / External Secrets
- **Registry** : GitHub Container Registry (GHCR) ou Harbor

---

## 2. Liste des services

| Service | Port | Rôle | BDD |
|---------|------|------|-----|
| `eureka-server` | 8761 | Service discovery | — |
| `config-server` | 8888 | Configuration centralisée | — (Git backend) |
| `api-gateway` | 8080 | Point d'entrée, routage, JWT | — |
| `auth-service` | 8081 | Authentification, émission JWT | `auth_db` |
| `user-service` | 8082 | Profils utilisateurs | `user_db` |
| `product-service` | 8083 | Catalogue produits | `product_db` |
| `inventory-service` | 8084 | Stock, réservations | `inventory_db` |
| `order-service` | 8085 | Commandes + orchestrateur saga | `order_db` |
| `payment-service` | 8086 | Paiements | `payment_db` |
| `notification-service` | 8087 | Notifications (email/SMS) | `notification_db` |

### 2.1 Groupes / artefacts Maven

- **groupId** : `com.microservices`
- **artifactId** : `<nom-du-service>` (ex : `order-service`)
- **Package racine** : `com.microservices.<nom_sans_tiret>` (ex : `com.microservices.order`)
- **Version** : héritée du parent (`1.0.0-SNAPSHOT`)

---

## 3. Conventions REST

### 3.1 URLs

```
/api/v1/<resource>              → collection
/api/v1/<resource>/{id}         → élément
/api/v1/<resource>/{id}/<sub>   → sous-ressource
```

Exemples :
```
GET    /api/v1/orders
GET    /api/v1/orders/{id}
POST   /api/v1/orders
PUT    /api/v1/orders/{id}
PATCH  /api/v1/orders/{id}
DELETE /api/v1/orders/{id}
GET    /api/v1/orders/{id}/items
```

### 3.2 Codes HTTP standards

| Code | Usage |
|------|-------|
| 200 | Succès (GET, PUT, PATCH) |
| 201 | Création (POST) + header `Location` |
| 204 | Suppression (DELETE) |
| 400 | Requête invalide |
| 401 | Non authentifié |
| 403 | Non autorisé |
| 404 | Ressource introuvable |
| 409 | Conflit (doublon, état invalide) |
| 422 | Erreur métier |
| 429 | Trop de requêtes (rate limit) |
| 500 | Erreur serveur |
| 503 | Service indisponible (circuit ouvert) |

### 3.3 Pagination

Query params standards :
```
?page=0&size=20&sort=createdAt,desc
```

Réponse paginée (Spring `Page<T>`) :
```json
{
  "content": [...],
  "page": 0,
  "size": 20,
  "totalElements": 152,
  "totalPages": 8,
  "last": false
}
```

### 3.4 Format d'erreur — RFC 7807 (Problem Details)

**Toutes les erreurs** doivent respecter ce format :

```json
{
  "type": "https://api.example.com/errors/validation",
  "title": "Validation failed",
  "status": 400,
  "detail": "Le champ 'email' doit être valide",
  "instance": "/api/v1/users",
  "timestamp": "2025-01-15T10:30:00Z",
  "traceId": "a1b2c3d4e5f6",
  "errors": [
    { "field": "email", "message": "Format invalide" }
  ]
}
```

Content-Type : `application/problem+json`

---

## 4. Conventions de nommage

### 4.1 Packages Java

```
com.microservices.<service>
├── config/            → @Configuration
├── controller/        → @RestController
├── service/           → logique métier
├── repository/        → Spring Data JPA
├── entity/            → entités JPA
├── dto/               → Data Transfer Objects
│   ├── request/
│   └── response/
├── mapper/            → MapStruct
├── exception/         → exceptions + @ControllerAdvice
├── client/            → WebClient clients vers autres services
├── event/             → producers/consumers Kafka
│   ├── producer/
│   └── consumer/
└── security/          → config sécurité locale
```

### 4.2 Tables PostgreSQL

- **snake_case**, pluriel
- Préfixe optionnel par domaine si collision : ex `users`, `orders`, `order_items`
- Colonnes : `id` (UUID ou BIGSERIAL), `created_at`, `updated_at`, `version` (optimistic lock)
- FK internes uniquement (jamais cross-service)

### 4.3 Topics Kafka

Format : `<domaine>.<entité>.<événement>` en **kebab-case** :

```
auth.user.registered
user.profile.updated
product.product.created
product.product.updated
inventory.stock.reserved
inventory.stock.released
inventory.stock.failed
order.order.created
order.order.confirmed
order.order.cancelled
payment.payment.completed
payment.payment.failed
payment.payment.refunded
notification.email.requested
```

**Conventions** :
- Nommage en **passé** pour les événements (fait accompli)
- 1 topic = 1 type d'événement
- Clé du message = `aggregateId` (ex : `orderId`) pour garantir l'ordre par agrégat
- Partitions : 3 minimum en dev, 6+ en prod
- Consumer groups : `<service>-<topic>-cg` (ex : `notification-order-created-cg`)

### 4.4 Branches Git

- `main` : production
- `develop` : intégration
- `feature/<ticket>-<slug>` : ex `feature/MSP-42-add-refresh-token`
- `hotfix/<ticket>-<slug>`
- `release/<version>`

### 4.5 Commits — Conventional Commits

```
feat(order): add saga orchestration for payment
fix(auth): correct token expiration check
docs(arch): update kafka topics list
chore(deps): bump spring-boot to 3.3.5
refactor(user): extract mapper to dedicated class
test(payment): add idempotency integration test
```

---

## 5. Bases de données PostgreSQL

### 5.1 Principe : 1 BDD par service

**Aucun partage de BDD.** Aucune FK cross-service. Communication uniquement via API ou Kafka.

| BDD | Service | Utilisateur | Schéma |
|-----|---------|-------------|--------|
| `auth_db` | auth-service | `auth_user` | `public` |
| `user_db` | user-service | `user_user` | `public` |
| `product_db` | product-service | `product_user` | `public` |
| `inventory_db` | inventory-service | `inventory_user` | `public` |
| `order_db` | order-service | `order_user` | `public` |
| `payment_db` | payment-service | `payment_user` | `public` |
| `notification_db` | notification-service | `notification_user` | `public` |

### 5.2 Migrations — Flyway

- **Flyway** obligatoire (pas de `ddl-auto: update`)
- `spring.jpa.hibernate.ddl-auto=validate` en prod
- Emplacement : `src/main/resources/db/migration/`
- Nommage : `V<version>__<description>.sql` (ex : `V1__init_users_table.sql`)
- Une migration = un changement atomique

### 5.3 Identifiants

- **UUID v7** (ou v4) pour les IDs exposés en API (évite l'énumération)
- Alternative : `BIGSERIAL` interne + UUID public (`public_id`)
- **Décision retenue** : UUID v4 en PK pour tous les services

---

## 6. Kafka

### 6.1 Topics (liste complète initiale)

| Topic | Producer | Consumers | Clé |
|-------|----------|-----------|-----|
| `auth.user.registered` | auth | user, notification | userId |
| `user.profile.updated` | user | notification | userId |
| `product.product.created` | product | inventory | productId |
| `product.product.updated` | product | inventory | productId |
| `inventory.stock.reserved` | inventory | order | orderId |
| `inventory.stock.released` | inventory | order | orderId |
| `inventory.stock.failed` | inventory | order | orderId |
| `order.order.created` | order | payment, notification | orderId |
| `order.order.confirmed` | order | notification | orderId |
| `order.order.cancelled` | order | inventory, payment, notification | orderId |
| `payment.payment.completed` | payment | order, notification | orderId |
| `payment.payment.failed` | payment | order, notification | orderId |
| `payment.payment.refunded` | payment | notification | orderId |

### 6.2 Format d'événement (enveloppe standard)

```json
{
  "eventId": "uuid",
  "eventType": "order.order.created",
  "eventVersion": "1.0",
  "aggregateId": "order-uuid",
  "occurredAt": "2025-01-15T10:30:00Z",
  "traceId": "trace-id",
  "payload": { ... }
}
```

### 6.3 Garanties

- **At-least-once** : les consumers doivent être **idempotents**
- **Outbox Pattern** : publication Kafka fiable via table `outbox_events` en BDD du producer, puis relay (Debezium ou scheduler)
- **Consumer idempotency** : table `processed_events(event_id)` dans chaque consumer

### 6.4 Serialization

- **JSON** (Jackson) pour démarrer
- Évolution vers **Avro + Schema Registry** en phase de scalabilité

---

## 7. Sécurité JWT

### 7.1 Modèle

- **Access token** : JWT signé **RS256** (asymétrique), durée **15 min**
- **Refresh token** : opaque, stocké en BDD `auth_db`, durée **7 jours**, rotation à chaque usage
- **Clés RSA** : générées au démarrage du `auth-service` (dev) ou fournies via secret K8s (prod)
- **JWKS endpoint** : `auth-service` expose `/.well-known/jwks.json` pour permettre aux autres services de valider les tokens

### 7.2 Claims du JWT

```json
{
  "sub": "user-uuid",
  "iss": "auth-service",
  "aud": "microservices-platform",
  "iat": 1705312800,
  "exp": 1705313700,
  "jti": "token-uuid",
  "roles": ["ROLE_CUSTOMER"],
  "email": "user@example.com",
  "scope": "read write"
}
```

### 7.3 Rôles

| Rôle | Description |
|------|-------------|
| `ROLE_ADMIN` | Administrateur plateforme |
| `ROLE_CUSTOMER` | Client standard |
| `ROLE_SUPPORT` | Support client (lecture commandes) |
| `ROLE_SERVICE` | Appels inter-services internes |

### 7.4 Propagation

- **Gateway** : valide le JWT à l'entrée, injecte les headers `X-User-Id`, `X-User-Roles`, `X-Trace-Id` vers les services internes.
- **Services** : ne revalident PAS le JWT (déjà validé par la Gateway). Ils font confiance aux headers `X-User-*` **uniquement si la requête vient du réseau interne** (à sécuriser via NetworkPolicy K8s / mTLS).
- **Appels inter-services** : chaque service expose un endpoint interne sous `/internal/**`, protégé par un token `ROLE_SERVICE` ou par mTLS.

### 7.5 Endpoints publics (whitelist Gateway)

```
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/refresh
GET  /api/v1/products/**        (lecture catalogue publique)
GET  /actuator/health
```

---

## 8. Communication inter-services (WebClient)

### 8.1 Principe

- **WebClient** (Spring WebFlux) pour appels synchrones
- Configuration centralisée avec :
    - **Load balancing** : `@LoadBalanced WebClient.Builder` (via Eureka)
    - **Timeout** : connect 2s, read 5s
    - **Retry** : 2 tentatives sur erreurs réseau uniquement
    - **Circuit Breaker** : Resilience4j
    - **Propagation du contexte** : `X-Trace-Id`, `X-User-Id`, `X-User-Roles`

### 8.2 Clients déclarés

| Depuis | Vers | Usage |
|--------|------|-------|
| order-service | product-service | Vérifier produit + prix |
| order-service | inventory-service | Réserver stock |
| order-service | payment-service | Demander paiement |
| order-service | user-service | Vérifier utilisateur |
| payment-service | user-service | Vérifier utilisateur |

### 8.3 Interfaces internes

Chaque service expose ses endpoints internes sous `/internal/v1/**`, non exposés par la Gateway.

Exemple :
```
GET  /internal/v1/products/{id}
POST /internal/v1/inventory/reserve
POST /internal/v1/payments/charge
```

---

## 9. Saga orchestrée — `order-service`

### 9.1 Principe

`order-service` est l'**orchestrateur**. Il pilote les étapes de la commande via des appels synchrones (WebClient) et des événements Kafka.

### 9.2 Étapes de la saga `PlaceOrderSaga`

```
1. Créer commande (état PENDING)                    [order_db]
2. → product-service : valider produits              [sync]
3. → inventory-service : réserver stock              [sync]
   ↳ si échec → compensation : annuler commande
4. → payment-service : débiter                       [sync]
   ↳ si échec → compensation : libérer stock + annuler
5. Confirmer commande (état CONFIRMED)               [order_db]
6. Publier order.order.confirmed → Kafka
   ↳ notification-service consomme → envoie email
```

### 9.3 États d'une commande

```
PENDING → RESERVED → PAID → CONFIRMED
   │         │        │
   └─────────┴────────┴──────► CANCELLED
```

### 9.4 Persistance de la saga

Table `saga_state` dans `order_db` :
```sql
CREATE TABLE saga_state (
  id UUID PRIMARY KEY,
  order_id UUID NOT NULL,
  step VARCHAR(50) NOT NULL,
  status VARCHAR(20) NOT NULL,  -- STARTED, COMPLETED, FAILED, COMPENSATING, COMPENSATED
  payload JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

### 9.5 Idempotency

- Chaque requête `POST /api/v1/orders` doit accepter un header `Idempotency-Key`
- Stocké dans table `idempotency_keys(key, response, created_at)`
- Si clé déjà vue → retourner la réponse précédente

---

## 10. Observabilité

### 10.1 Logs

- Format **JSON structuré** (Logback + `logstash-logback-encoder`)
- Champs obligatoires : `timestamp`, `level`, `service`, `traceId`, `spanId`, `userId`, `message`
- Corrélation via **MDC** : `traceId`, `spanId`, `userId`

### 10.2 Tracing distribué

- **Micrometer Tracing** + **Brave** + **Zipkin**
- Propagation via headers W3C `traceparent`
- Chaque appel HTTP/Kafka doit propager le contexte

### 10.3 Métriques

- **Micrometer** + **Prometheus** + **Grafana**
- Endpoint : `/actuator/prometheus`
- Dashboards : latence, taux d'erreur, saturation, débit (RED method)
- Alertes : circuit ouvert > 1 min, taux erreur 5xx > 1%

### 10.4 Health checks

- `/actuator/health` (liveness + readiness pour K8s)
- `/actuator/health/liveness`
- `/actuator/health/readiness`

---

## 11. Ports et variables d'environnement

### 11.1 Ports

| Composant | Port |
|-----------|------|
| API Gateway | 8080 |
| auth-service | 8081 |
| user-service | 8082 |
| product-service | 8083 |
| inventory-service | 8084 |
| order-service | 8085 |
| payment-service | 8086 |
| notification-service | 8087 |
| Config Server | 8888 |
| Eureka Server | 8761 |
| PostgreSQL | 5432 |
| Kafka | 9092 |
| Zipkin | 9411 |
| Prometheus | 9090 |
| Grafana | 3000 |

### 11.2 Variables d'environnement standard

```bash
# Spring
SPRING_PROFILES_ACTIVE=dev
SPRING_CONFIG_IMPORT=optional:configserver:http://config-server:8888

# Eureka
EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka-server:8761/eureka

# Base de données
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-<service>:5432/<db>
SPRING_DATASOURCE_USERNAME=<user>
SPRING_DATASOURCE_PASSWORD=<password>

# Kafka
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092

# JWT
JWT_PRIVATE_KEY=<base64>
JWT_PUBLIC_KEY=<base64>
JWT_ACCESS_TOKEN_TTL=900
JWT_REFRESH_TOKEN_TTL=604800

# Tracing
MANAGEMENT_ZIPKIN_TRACING_ENDPOINT=http://zipkin:9411/api/v2/spans
```

---

## 12. Arborescence cible du projet

```
microservices-platform/
├── pom.xml
├── README.md
├── .gitignore
├── docs/
│   ├── ARCHITECTURE.md
│   ├── API.md
│   └── RUNBOOK.md
├── infrastructure/
│   ├── pom.xml
│   ├── eureka-server/
│   ├── config-server/
│   │   └── src/main/resources/config-repo/   (configs YAML des services)
│   └── api-gateway/
├── services/
│   ├── pom.xml
│   ├── auth-service/
│   ├── user-service/
│   ├── product-service/
│   ├── inventory-service/
│   ├── order-service/
│   ├── payment-service/
│   └── notification-service/
├── shared/
│   ├── pom.xml
│   ├── common-dto/
│   ├── common-events/
│   └── common-security/
├── docker/
│   ├── docker-compose.yml
│   ├── docker-compose.infra.yml
│   └── .env
└── k8s/
    ├── base/
    └── overlays/
        ├── dev/
        └── prod/
```

---

## 13. Décisions techniques (ADR condensés)

| # | Décision | Justification | Alternatives rejetées |
|---|----------|---------------|----------------------|
| 1 | Maven multi-modules | Standard entreprise, gestion centralisée | Gradle (courbe d'apprentissage) |
| 2 | Java 21 | LTS, virtual threads, pattern matching | Java 17 (moins récent) |
| 3 | Spring Boot 3.3 + Spring Cloud 2023.0 | Dernières versions stables | — |
| 4 | PostgreSQL 16 | ACID, JSONB, mature | MySQL, MongoDB |
| 5 | 1 BDD par service | Isolation, scalabilité | BDD partagée (anti-pattern) |
| 6 | WebClient pour sync | Réactif, moderne, non-bloquant | RestTemplate (deprecated), Feign (bloquant) |
| 7 | Kafka (at-least-once) | Découplage, scalabilité, replay | RabbitMQ (moins scalable) |
| 8 | Saga orchestrée | Contrôle central, debug facile | Chorégraphiée (complexe à tracer) |
| 9 | JWT RS256 | Vérification décentralisée via JWKS | HS256 (secret partagé) |
| 10 | Outbox Pattern | Fiabilité publication Kafka | Publication directe (risque de perte) |
| 11 | UUID v4 en PK | Non-énumérable, distribué | BIGSERIAL (séquentiel) |
| 12 | Flyway | Migrations versionnées, rollback | Liquibase, Hibernate ddl-auto |
| 13 | MapStruct | Mapping performant, compile-time | ModelMapper (runtime, lent) |
| 14 | RFC 7807 | Standard erreurs HTTP | Format custom |
| 15 | K8s en cible | Standard industriel | Docker Swarm (obsolète) |

---

## 14. Roadmap d'implémentation

- [x] Étape 1 — Projet parent Maven
- [x] Étape 2 — Architecture globale (ce document)
- [ ] Étape 3 — Eureka Server
- [ ] Étape 4 — Config Server
- [ ] Étape 5 — API Gateway
- [ ] Étape 6 — auth-service (JWT)
- [ ] Étape 7 — user-service
- [ ] Étape 8 — product-service
- [ ] Étape 9 — inventory-service
- [ ] Étape 10 — order-service + saga
- [ ] Étape 11 — payment-service
- [ ] Étape 12 — notification-service
- [ ] Étape 13 — Kafka (topics + producers/consumers)
- [ ] Étape 14 — WebClient + Resilience4j
- [ ] Étape 15 — Observabilité (logs, tracing, metrics)
- [ ] Étape 16 — Tests
- [ ] Étape 17 — Dockerisation
- [ ] Étape 18 — Docker Compose
- [ ] Étape 19 — CI/CD
- [ ] Étape 20 — Kubernetes
- [ ] Étape 21 — Outbox + Idempotency
- [ ] Étape 22 — Sécurité production

---

## 15. Références

- [Spring Cloud Docs](https://docs.spring.io/spring-cloud/docs/current/reference/html/)
- [12-Factor App](https://12factor.net/)
- [RFC 7807 — Problem Details](https://datatracker.ietf.org/doc/html/rfc7807)
- [Microservices Patterns — Chris Richardson](https://microservices.io/patterns/)
- [Conventional Commits](https://www.conventionalcommits.org/)