# Microservices Platform

Plateforme microservices basée sur **Spring Boot 3.3.5** et **Spring Cloud 2023.0.3**.

## Stack

- Java 21 (ou 17)
- Maven multi-modules
- Spring Boot 3 / Spring Cloud
- PostgreSQL (une base par service)
- Apache Kafka
- JWT (sécurité)
- Eureka, Config Server, API Gateway
- Docker / Kubernetes

## Modules

### Infrastructure
- `eureka-server` : service discovery
- `config-server` : configuration centralisée
- `api-gateway` : point d'entrée unique

### Services métier
- `auth-service`
- `user-service`
- `product-service`
- `inventory-service`
- `order-service`
- `payment-service`
- `notification-service`

### Shared
- `common-dto`
- `common-events`
- `common-security`

## Build

```bash
mvn clean install

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [API](docs/API.md)
- [Runbook](docs/RUNBOOK.md)

## Roadmap

Voir section 14 de `docs/ARCHITECTURE.md`.