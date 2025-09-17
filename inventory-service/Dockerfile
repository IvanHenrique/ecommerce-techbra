FROM maven:3.9.5-openjdk-21-slim AS builder

WORKDIR /app

COPY pom.xml .
COPY ../shared-kernel/pom.xml shared-kernel/
COPY ../pom.xml ../

RUN mvn dependency:go-offline -B

COPY src ./src
COPY ../shared-kernel/src ./shared-kernel/src

RUN mvn clean package -DskipTests -B

FROM gcr.io/distroless/java21-debian12:nonroot

WORKDIR /app

COPY --from=builder /app/target/inventory-service-*.jar app.jar

USER nonroot:nonroot

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8083/actuator/health || exit 1

ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:+UseContainerSupport"
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]