# ============================================================
# Stage 1 : Build
# ============================================================
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

# Copie TOUS les POMs (pour que Maven lise la structure complète)
COPY pom.xml ./
COPY shared/pom.xml ./shared/
COPY infrastructure/pom.xml ./infrastructure/
COPY infrastructure/eureka-server/pom.xml ./infrastructure/eureka-server/
COPY infrastructure/config-server/pom.xml ./infrastructure/config-server/
COPY infrastructure/api-gateway/pom.xml ./infrastructure/api-gateway/
COPY services/pom.xml ./services/
COPY services/auth-service/pom.xml ./services/auth-service/
COPY services/user-service/pom.xml ./services/user-service/
COPY services/product-service/pom.xml ./services/product-service/
COPY services/inventory-service/pom.xml ./services/inventory-service/
COPY services/order-service/pom.xml ./services/order-service/
COPY services/payment-service/pom.xml ./services/payment-service/
COPY services/notification-service/pom.xml ./services/notification-service/

# Télécharge les dépendances (cache Docker)
RUN mvn dependency:go-offline -B -pl infrastructure/eureka-server -am || true

# Copie les sources
COPY shared ./shared
COPY infrastructure ./infrastructure
COPY services ./services

# Build
RUN mvn clean package -DskipTests -pl infrastructure/eureka-server -am

# ============================================================
# Stage 2 : Runtime
# ============================================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring

COPY --from=builder /build/infrastructure/eureka-server/target/*.jar app.jar

EXPOSE 8761

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8761/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]