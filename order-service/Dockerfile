FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copiar todos os módulos para satisfazer o pom.xml pai
COPY pom.xml .
COPY shared-kernel/ shared-kernel/
COPY order-service/ order-service/
COPY billing-service/ billing-service/
COPY inventory-service/ inventory-service/
COPY bff/ bff/

# Build apenas do order-service
RUN mvn clean package -pl order-service -am -DskipTests

# Runtime stage
FROM openjdk:21-jdk-slim

WORKDIR /app

COPY --from=builder /app/order-service/target/order-service-*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:dev}", "-jar", "app.jar"]