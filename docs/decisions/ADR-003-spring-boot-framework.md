# ADR-003: Adoção do Spring Boot como Framework Principal

## Status
**Aceita** - 2024-01-15

## Contexto

Para o desenvolvimento dos microserviços do sistema e-commerce Techbra, precisamos escolher um framework que ofereça:

- Desenvolvimento rápido e produtivo
- Ecossistema maduro e bem documentado
- Suporte nativo a microserviços
- Integração com ferramentas de observabilidade
- Comunidade ativa e suporte empresarial
- Facilidade de testes e manutenção

As opções consideradas foram:

1. **Spring Boot** (Java)
2. **Quarkus** (Java)
3. **Micronaut** (Java)
4. **Node.js** com Express
5. **Go** com Gin/Echo
6. **Python** com FastAPI

## Decisão

Adotamos **Spring Boot 3.x** como framework principal para todos os microserviços do sistema e-commerce Techbra.

### Versões e Dependências

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.1</version>
    <relativePath/>
</parent>

<properties>
    <java.version>17</java.version>
    <spring-cloud.version>2023.0.0</spring-cloud.version>
    <testcontainers.version>1.19.3</testcontainers.version>
</properties>
```

## Justificativa da Decisão

### Comparação Detalhada

| Critério | Spring Boot | Quarkus | Micronaut | Node.js | Go | Python |
|----------|-------------|---------|-----------|---------|----|---------|
| **Maturidade** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ |
| **Ecossistema** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ |
| **Performance** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| **Startup Time** | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **Memory Usage** | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| **Developer Experience** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ |
| **Testing** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ |
| **Observabilidade** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ |
| **Comunidade** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **Curva de Aprendizado** | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |

### Vantagens do Spring Boot

✅ **Ecossistema Completo**
- Spring Data JPA para persistência
- Spring Security para autenticação/autorização
- Spring Cloud para microserviços
- Spring Kafka para mensageria
- Spring Actuator para observabilidade

✅ **Produtividade de Desenvolvimento**
- Auto-configuração inteligente
- Starter dependencies
- DevTools para hot reload
- Spring Boot CLI

✅ **Maturidade e Estabilidade**
- Framework maduro com 10+ anos
- Backward compatibility
- Suporte LTS (Long Term Support)
- Documentação extensa

✅ **Observabilidade Nativa**
- Micrometer para métricas
- Distributed tracing com Sleuth
- Health checks automáticos
- Prometheus endpoints

✅ **Testing Framework**
- @SpringBootTest para testes de integração
- TestContainers integration
- MockMvc para testes de API
- Profiles para diferentes ambientes

## Implementação

### Estrutura Base dos Microserviços

```java
@SpringBootApplication
@EnableJpaRepositories
@EnableKafka
@EnableScheduling
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
```

### Configuração Padrão

**application.yml**
```yaml
spring:
  application:
    name: ${SERVICE_NAME:order-service}
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}
  
  # Database
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/orderdb}
    username: ${DATABASE_USERNAME:postgres}
    password: ${DATABASE_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: ${DDL_AUTO:validate}
    show-sql: ${SHOW_SQL:false}
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
  
  # Kafka
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: ${spring.application.name}
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "com.techbra.ecommerce"

# Actuator
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true

# Logging
logging:
  level:
    com.techbra.ecommerce: ${LOG_LEVEL:INFO}
    org.springframework.kafka: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level [%X{traceId},%X{spanId}] %logger{36} - %msg%n"
```

### Dependências Principais

**pom.xml**
```xml
<dependencies>
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    
    <!-- Spring Kafka -->
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>
    
    <!-- Database -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
    </dependency>
    
    <!-- Observability -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-tracing-bridge-brave</artifactId>
    </dependency>
    
    <!-- Testing -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>postgresql</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### Padrões Arquiteturais

**Hexagonal Architecture**
```java
// Domain
@Entity
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long customerId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    
    // Domain methods
    public void confirm() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("Only pending orders can be confirmed");
        }
        this.status = OrderStatus.CONFIRMED;
    }
}

// Application Service
@Service
@Transactional
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;
    
    public Order createOrder(CreateOrderCommand command) {
        Order order = new Order(command.getCustomerId(), command.getItems());
        Order savedOrder = orderRepository.save(order);
        
        eventPublisher.publishOrderCreated(savedOrder);
        return savedOrder;
    }
}

// Infrastructure - Repository
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerId(Long customerId);
    List<Order> findByStatus(OrderStatus status);
}

// Infrastructure - Event Publisher
@Component
public class OrderEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    public void publishOrderCreated(Order order) {
        OrderCreatedEvent event = new OrderCreatedEvent(order);
        kafkaTemplate.send("order-events", event);
    }
}

// Adapter - REST Controller
@RestController
@RequestMapping("/api/orders")
@Validated
public class OrderController {
    private final OrderService orderService;
    
    @PostMapping
    public ResponseEntity<OrderDto> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        CreateOrderCommand command = new CreateOrderCommand(request);
        Order order = orderService.createOrder(command);
        return ResponseEntity.ok(OrderDto.from(order));
    }
}
```

## Consequências

### Positivas

✅ **Produtividade Alta**
- Desenvolvimento rápido com auto-configuração
- Menos boilerplate code
- Convenções bem estabelecidas

✅ **Ecossistema Rico**
- Integração nativa com ferramentas populares
- Ampla gama de starters disponíveis
- Suporte oficial para cloud providers

✅ **Observabilidade Completa**
- Métricas automáticas com Micrometer
- Health checks configuráveis
- Distributed tracing integrado

✅ **Testing Robusto**
- Framework de testes abrangente
- Suporte nativo a TestContainers
- Mocking e profiling avançados

✅ **Segurança**
- Spring Security integrado
- OAuth2 e JWT support
- HTTPS e CORS configuráveis

✅ **Comunidade e Suporte**
- Documentação extensa
- Comunidade ativa
- Suporte comercial disponível

### Negativas

❌ **Startup Time**
- Tempo de inicialização mais lento que frameworks nativos
- Impacto em cold starts (serverless)

❌ **Memory Footprint**
- Maior consumo de memória
- Overhead do framework

❌ **Complexidade**
- Curva de aprendizado para iniciantes
- "Magic" da auto-configuração pode confundir

❌ **Vendor Lock-in**
- Dependência do ecossistema Spring
- Migração para outros frameworks é complexa

## Mitigações

### Performance
- **GraalVM Native Image**: Para reduzir startup time e memory
- **Spring Boot 3.x**: Melhorias de performance
- **JVM Tuning**: Otimização de parâmetros da JVM

```dockerfile
# Dockerfile otimizado
FROM eclipse-temurin:17-jre-alpine

# JVM tuning
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication"

COPY target/order-service.jar app.jar
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app.jar"]
```

### Complexidade
- **Documentação Interna**: Guias e padrões da equipe
- **Code Reviews**: Revisões focadas em boas práticas
- **Training**: Treinamento da equipe em Spring Boot

### Vendor Lock-in
- **Hexagonal Architecture**: Isolamento da lógica de negócio
- **Interfaces**: Abstrações para componentes externos
- **Shared Kernel**: Código comum independente do framework

## Padrões e Convenções

### Estrutura de Pacotes
```
com.techbra.ecommerce.order/
├── application/          # Application Services
│   ├── command/         # Commands e Command Handlers
│   ├── query/           # Queries e Query Handlers
│   └── service/         # Application Services
├── domain/              # Domain Layer
│   ├── entity/          # Entities
│   ├── repository/      # Repository Interfaces
│   └── service/         # Domain Services
├── infrastructure/      # Infrastructure Layer
│   ├── config/          # Configurações
│   ├── persistence/     # JPA Repositories
│   ├── messaging/       # Kafka Producers/Consumers
│   └── external/        # Clientes externos
└── web/                 # Web Layer
    ├── controller/      # REST Controllers
    ├── dto/             # DTOs
    └── exception/       # Exception Handlers
```

### Configuração por Ambiente

**application-local.yml**
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
  h2:
    console:
      enabled: true
logging:
  level:
    com.techbra.ecommerce: DEBUG
```

**application-prod.yml**
```yaml
spring:
  datasource:
    url: ${DATABASE_URL}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
logging:
  level:
    com.techbra.ecommerce: INFO
```

## Métricas de Sucesso

- **Development Velocity**: Redução de 40% no tempo de desenvolvimento
- **Code Quality**: Cobertura de testes > 80%
- **Startup Time**: < 30 segundos em produção
- **Memory Usage**: < 512MB por instância
- **API Response Time**: < 200ms para 95% das requisições
- **Developer Satisfaction**: Score > 4.0/5.0 em pesquisas

## Evolução Futura

### Spring Boot 3.x Features
- **Native Image**: Compilação nativa com GraalVM
- **Virtual Threads**: Project Loom integration
- **Observability**: OpenTelemetry integration
- **Jakarta EE**: Migração para Jakarta namespace

### Próximas Versões
- **Spring Boot 3.3**: Melhorias de performance
- **Spring Framework 6.x**: Reactive programming
- **Spring Cloud 2024.x**: Service mesh integration

## Revisão

Esta decisão deve ser revisada:
- **Semestralmente**: Avaliação de performance e produtividade
- **Anualmente**: Comparação com frameworks alternativos
- **Major Releases**: Avaliação de novas features do Spring Boot

## Referências

- [Spring Boot Reference Documentation](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/)
- [Spring Boot Best Practices](https://spring.io/guides)
- [Microservices with Spring Boot](https://spring.io/microservices)
- [Spring Boot Performance Tuning](https://spring.io/blog/2023/10/16/runtime-efficiency-with-spring)