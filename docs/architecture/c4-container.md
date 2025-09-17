# C4 Model - Container Diagram

## Visão Geral

O Container Diagram mostra a arquitetura interna do sistema E-commerce Techbra, detalhando os microserviços, bancos de dados e suas interações.

## Diagrama de Containers

```mermaid
C4Container
    title Sistema E-commerce Techbra - Container Diagram
    
    Person(customer, "Cliente", "Usuário final")
    Person(admin, "Administrador", "Usuário interno")
    
    Container_Boundary(c1, "E-commerce Techbra") {
        Container(bff, "BFF", "Spring Boot", "Backend for Frontend - Agrega dados dos microserviços")
        
        Container(order_service, "Order Service", "Spring Boot", "Gerencia pedidos e orquestração")
        Container(billing_service, "Billing Service", "Spring Boot", "Processa pagamentos")
        Container(inventory_service, "Inventory Service", "Spring Boot", "Gerencia estoque")
        
        ContainerDb(postgres, "PostgreSQL", "Database", "Armazena dados dos microserviços")
        Container(redis, "Redis", "Cache", "Cache distribuído")
        Container(kafka, "Apache Kafka", "Message Broker", "Mensageria assíncrona")
    }
    
    Container_Boundary(c2, "Observabilidade") {
        Container(prometheus, "Prometheus", "Monitoring", "Coleta métricas")
        Container(grafana, "Grafana", "Dashboard", "Visualização de métricas")
        Container(jaeger, "Jaeger", "Tracing", "Distributed tracing")
    }
    
    System_Ext(payment_gateway, "Gateway de Pagamento", "Processa pagamentos")
    System_Ext(notification_service, "Serviço de Notificação", "Envia notificações")
    
    Rel(customer, bff, "HTTPS/REST")
    Rel(admin, bff, "HTTPS/REST")
    
    Rel(bff, order_service, "HTTP/REST")
    Rel(bff, billing_service, "HTTP/REST")
    Rel(bff, inventory_service, "HTTP/REST")
    
    Rel(order_service, postgres, "JDBC")
    Rel(billing_service, postgres, "JDBC")
    Rel(inventory_service, postgres, "JDBC")
    
    Rel(order_service, redis, "Redis Protocol")
    Rel(billing_service, redis, "Redis Protocol")
    Rel(inventory_service, redis, "Redis Protocol")
    
    Rel(order_service, kafka, "Kafka Protocol")
    Rel(billing_service, kafka, "Kafka Protocol")
    Rel(inventory_service, kafka, "Kafka Protocol")
    
    Rel(billing_service, payment_gateway, "HTTPS/REST")
    Rel(order_service, notification_service, "HTTPS/REST")
    
    Rel_Back(prometheus, order_service, "Scrape metrics")
    Rel_Back(prometheus, billing_service, "Scrape metrics")
    Rel_Back(prometheus, inventory_service, "Scrape metrics")
    Rel_Back(prometheus, bff, "Scrape metrics")
    
    Rel(grafana, prometheus, "PromQL")
    
    Rel_Back(jaeger, order_service, "Traces")
    Rel_Back(jaeger, billing_service, "Traces")
    Rel_Back(jaeger, inventory_service, "Traces")
    Rel_Back(jaeger, bff, "Traces")
```

## Containers

### Microserviços

| Container | Tecnologia | Porta | Responsabilidade |
|-----------|------------|-------|------------------|
| **BFF** | Spring Boot 3.2 | 8084 | - Agregação de dados<br>- Rate limiting<br>- Autenticação<br>- Proxy para microserviços |
| **Order Service** | Spring Boot 3.2 | 8081 | - Criação de pedidos<br>- Orquestração de saga<br>- Gestão de status |
| **Billing Service** | Spring Boot 3.2 | 8082 | - Processamento de pagamentos<br>- Integração com gateway<br>- Gestão de faturas |
| **Inventory Service** | Spring Boot 3.2 | 8083 | - Gestão de estoque<br>- Reserva de produtos<br>- Controle de disponibilidade |

### Infraestrutura

| Container | Tecnologia | Porta | Responsabilidade |
|-----------|------------|-------|------------------|
| **PostgreSQL** | PostgreSQL 15 | 5432 | - Persistência de dados<br>- Transações ACID<br>- Schemas por microserviço |
| **Redis** | Redis 7 | 6379 | - Cache de sessões<br>- Cache de consultas<br>- Rate limiting |
| **Apache Kafka** | Kafka 3.5 | 9092 | - Eventos de domínio<br>- Comunicação assíncrona<br>- Event sourcing |

### Observabilidade

| Container | Tecnologia | Porta | Responsabilidade |
|-----------|------------|-------|------------------|
| **Prometheus** | Prometheus | 9090 | - Coleta de métricas<br>- Alertas<br>- Time series database |
| **Grafana** | Grafana | 3000 | - Dashboards<br>- Visualização<br>- Alertas visuais |
| **Jaeger** | Jaeger | 16686 | - Distributed tracing<br>- Performance monitoring<br>- Dependency analysis |

## Padrões de Comunicação

### Síncrona (REST)
- **BFF ↔ Microserviços**: Agregação de dados
- **Microserviços ↔ Sistemas Externos**: Integrações
- **Cliente ↔ BFF**: APIs públicas

### Assíncrona (Kafka)
- **Order Created**: Order Service → Billing Service, Inventory Service
- **Payment Processed**: Billing Service → Order Service
- **Stock Reserved**: Inventory Service → Order Service
- **Order Completed**: Order Service → Notification Service

## Tópicos Kafka

| Tópico | Producer | Consumer | Evento |
|--------|----------|----------|--------|
| `order-events` | Order Service | Billing, Inventory | OrderCreated, OrderCancelled |
| `payment-events` | Billing Service | Order Service | PaymentProcessed, PaymentFailed |
| `inventory-events` | Inventory Service | Order Service | StockReserved, StockReleased |

## Schemas de Banco

### PostgreSQL
- **order_db**: Pedidos, itens, status
- **billing_db**: Pagamentos, faturas, transações
- **inventory_db**: Produtos, estoque, reservas

### Redis
- **Cache de sessões**: `session:*`
- **Cache de produtos**: `product:*`
- **Rate limiting**: `rate_limit:*`

## Características Técnicas

- **Resilência**: Circuit breaker, retry, timeout
- **Segurança**: JWT, HTTPS, secrets management
- **Escalabilidade**: Horizontal scaling, load balancing
- **Observabilidade**: Metrics, logs, traces
- **Deploy**: Docker + Kubernetes + Helm