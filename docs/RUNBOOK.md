# Runbook — Opérations

> Procédures d'exploitation, debug, incidents.

## Démarrage local

_(À compléter à l'étape Docker Compose)_

## Debug

### Un service ne s'enregistre pas dans Eureka
1. Vérifier `EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE`
2. Vérifier les logs `com.netflix.discovery`
3. Vérifier que `eureka-server` répond : `curl http://localhost:8761/actuator/health`

### Circuit breaker ouvert
1. Consulter `/actuator/circuitbreakers`
2. Identifier le service fautif via Grafana
3. Vérifier `/actuator/health` du service cible

### Kafka lag élevé
1. Consulter Kafka UI (`http://localhost:8090`)
2. Identifier le consumer group en retard
3. Scaler le consumer (augmenter replicas)

## Incidents

_(À compléter en production)_