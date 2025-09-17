# C4 Model - Component Diagram

## Visão Geral

O Component Diagram detalha os componentes internos de cada microserviço, mostrando a arquitetura hexagonal e os padrões utilizados.

## Order Service - Components

```mermaid
C4Component
    title Order Service - Component Diagram
    
    Container_Boundary(c1, "Order Service") {
        Component(order_controller, "OrderController", "Spring MVC", "REST endpoints para pedidos")
        Component(order_service, "OrderService", "Service Layer", "Lógica de negócio de pedidos")
        Component(saga_orchestrator, "SagaOrchestrator", "Saga Pattern", "Orquestra transações distribuídas")
        Component(order_repository, "OrderRepository", "Spring Data JPA", "Persistência de pedidos")
        Component(kafka_producer, "KafkaProducer", "Spring Kafka", "Publica eventos")
        Component(kafka_consumer, "KafkaConsumer", "Spring Kafka", "Consome eventos")
        Component(cache_service, "CacheService", "Spring Cache", "Cache de consultas")
        Component(metrics_service, "MetricsService", "Micrometer", "Métricas de negócio")
    }
    
    ContainerDb(postgres, "PostgreSQL", "Database", "order_db schema")
    Container(redis, "Redis", "Cache", "Cache distribuído")
    Container(kafka, "Kafka", "Message Broker", "Event streaming")
    
    Rel(order_controller, order_service, "Uses")
    Rel(order_service, saga_orchestrator, "Uses")
    Rel(order_service, order_repository, "Uses")
    Rel(order_service, cache_service, "Uses")
    Rel(order_service, metrics_service, "Uses")
    
    Rel(saga_orchestrator, kafka_producer, "Uses")
    Rel(kafka_consumer, order_service, "Calls")
    
    Rel(order_repository, postgres, "JDBC")
    Rel(cache_service, redis, "Redis Protocol")
    Rel(kafka_producer, kafka, "Produces")
    Rel(kafka_consumer, kafka, "Consumes")
```

## Billing Service - Components

```mermaid
C4Component
    title Billing Service - Component Diagram
    
    Container_Boundary(c2, "Billing Service") {
        Component(billing_controller, "BillingController", "Spring MVC", "REST endpoints para pagamentos")
        Component(payment_service, "PaymentService", "Service Layer", "Lógica de pagamentos")
        Component(payment_gateway_client, "PaymentGatewayClient", "Feign Client", "Integração com gateway")
        Component(billing_repository, "BillingRepository", "Spring Data JPA", "Persistência de pagamentos")
        Component(kafka_producer, "KafkaProducer", "Spring Kafka", "Publica eventos de pagamento")
        Component(kafka_consumer, "KafkaConsumer", "Spring Kafka", "Consome eventos de pedidos")
        Component(circuit_breaker, "CircuitBreaker", "Resilience4j", "Proteção contra falhas")
        Component(retry_service, "RetryService", "Resilience4j", "Retry automático")
    }
    
    ContainerDb(postgres, "PostgreSQL", "Database", "billing_db schema")
    Container(kafka, "Kafka", "Message Broker", "Event streaming")
    System_Ext(payment_gateway, "Payment Gateway", "Processador de pagamentos")
    
    Rel(billing_controller, payment_service, "Uses")
    Rel(payment_service, payment_gateway_client, "Uses")
    Rel(payment_service, billing_repository, "Uses")
    Rel(payment_service, kafka_producer, "Uses")
    
    Rel(payment_gateway_client, circuit_breaker, "Protected by")
    Rel(payment_gateway_client, retry_service, "Uses")
    Rel(kafka_consumer, payment_service, "Calls")
    
    Rel(billing_repository, postgres, "JDBC")
    Rel(payment_gateway_client, payment_gateway, "HTTPS")
    Rel(kafka_producer, kafka, "Produces")
    Rel(kafka_consumer, kafka, "Consumes")
```

## Inventory Service - Components

```mermaid
C4Component
    title Inventory Service - Component Diagram
    
    Container_Boundary(c3, "Inventory Service") {
        Component(inventory_controller, "InventoryController", "Spring MVC", "REST endpoints para estoque")
        Component(inventory_service, "InventoryService", "Service Layer", "Lógica de estoque")
        Component(stock_manager, "StockManager", "Domain Service", "Gerencia reservas de estoque")
        Component(inventory_repository, "InventoryRepository", "Spring Data JPA", "Persistência de estoque")
        Component(kafka_producer, "KafkaProducer", "Spring Kafka", "Publica eventos de estoque")
        Component(kafka_consumer, "KafkaConsumer", "Spring Kafka", "Consome eventos de pedidos")
        Component(cache_service, "CacheService", "Spring Cache", "Cache de produtos")
        Component(lock_service, "LockService", "Redisson", "Distributed locking")
    }
    
    ContainerDb(postgres, "PostgreSQL", "Database", "inventory_db schema")
    Container(redis, "Redis", "Cache", "Cache e locks distribuídos")
    Container(kafka, "Kafka", "Message Broker", "Event streaming")
    
    Rel(inventory_controller, inventory_service, "Uses")
    Rel(inventory_service, stock_manager, "Uses")
    Rel(inventory_service, inventory_repository, "Uses")
    Rel(inventory_service, cache_service, "Uses")
    
    Rel(stock_manager, lock_service, "Uses")
    Rel(stock_manager, kafka_producer, "Uses")
    Rel(kafka_consumer, inventory_service, "Calls")
    
    Rel(inventory_repository, postgres, "JDBC")
    Rel(cache_service, redis, "Redis Protocol")
    Rel(lock_service, redis, "Redis Protocol")
    Rel(kafka_producer, kafka, "Produces")
    Rel(kafka_consumer, kafka, "Consumes")
```

## BFF - Components

```mermaid
C4Component
    title BFF - Component Diagram
    
    Container_Boundary(c4, "BFF") {
        Component(order_controller, "OrderController", "Spring MVC", "Endpoints de pedidos")
        Component(product_controller, "ProductController", "Spring MVC", "Endpoints de produtos")
        Component(aggregation_service, "AggregationService", "Service Layer", "Agrega dados dos microserviços")
        Component(order_client, "OrderClient", "Feign Client", "Cliente do Order Service")
        Component(billing_client, "BillingClient", "Feign Client", "Cliente do Billing Service")
        Component(inventory_client, "InventoryClient", "Feign Client", "Cliente do Inventory Service")
        Component(rate_limiter, "RateLimiter", "Resilience4j", "Controle de taxa")
        Component(auth_service, "AuthService", "Spring Security", "Autenticação e autorização")
    }
    
    Container(order_service, "Order Service", "Microservice", "Gerencia pedidos")
    Container(billing_service, "Billing Service", "Microservice", "Processa pagamentos")
    Container(inventory_service, "Inventory Service", "Microservice", "Gerencia estoque")
    Container(redis, "Redis", "Cache", "Rate limiting")
    
    Rel(order_controller, aggregation_service, "Uses")
    Rel(product_controller, aggregation_service, "Uses")
    
    Rel(aggregation_service, order_client, "Uses")
    Rel(aggregation_service, billing_client, "Uses")
    Rel(aggregation_service, inventory_client, "Uses")
    
    Rel(order_controller, auth_service, "Protected by")
    Rel(product_controller, auth_service, "Protected by")
    Rel(order_controller, rate_limiter, "Protected by")
    
    Rel(order_client, order_service, "HTTP")
    Rel(billing_client, billing_service, "HTTP")
    Rel(inventory_client, inventory_service, "HTTP")
    Rel(rate_limiter, redis, "Redis Protocol")
```

## Padrões Arquiteturais

### Arquitetura Hexagonal
Cada microserviço segue a arquitetura hexagonal (ports and adapters):

- **Ports (Interfaces)**:
  - Inbound: Controllers, Event Listeners
  - Outbound: Repositories, External Clients

- **Adapters (Implementações)**:
  - Inbound: REST Controllers, Kafka Consumers
  - Outbound: JPA Repositories, Feign Clients

- **Core (Domínio)**:
  - Services, Domain Objects, Business Rules

### Domain-Driven Design (DDD)

| Microserviço | Bounded Context | Agregados | Entidades |
|--------------|-----------------|-----------|----------|
| **Order Service** | Order Management | Order, OrderItem | Order, Customer |
| **Billing Service** | Payment Processing | Payment, Invoice | Payment, Transaction |
| **Inventory Service** | Stock Management | Product, Stock | Product, StockReservation |

### Padrões de Integração

- **Saga Pattern**: Transações distribuídas (Order Service)
- **Circuit Breaker**: Proteção contra falhas (Billing Service)
- **Event Sourcing**: Histórico de eventos (Kafka)
- **CQRS**: Separação de comandos e consultas
- **Distributed Locking**: Controle de concorrência (Inventory Service)

### Padrões de Resilência

- **Retry**: Tentativas automáticas
- **Timeout**: Limite de tempo para operações
- **Bulkhead**: Isolamento de recursos
- **Rate Limiting**: Controle de taxa de requisições

## Responsabilidades dos Componentes

### Controllers
- Validação de entrada
- Serialização/Deserialização
- Mapeamento de DTOs
- Tratamento de exceções

### Services
- Lógica de negócio
- Orquestração de operações
- Validações de domínio
- Coordenação de transações

### Repositories
- Persistência de dados
- Consultas otimizadas
- Mapeamento objeto-relacional
- Transações de banco

### Clients
- Comunicação entre serviços
- Serialização de requisições
- Tratamento de erros de rede
- Load balancing

### Event Handlers
- Processamento assíncrono
- Idempotência
- Dead letter queues
- Retry policies