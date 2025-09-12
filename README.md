# Projeto Microserviços - E-commerce Platform (Techbra)

## Visão Geral

Este projeto implementa uma plataforma de e-commerce baseada em microserviços utilizando arquitetura hexagonal, seguindo as melhores práticas de desenvolvimento com SOLID, DRY e YAGNI.

A aplicação utiliza **Maven Multi-Module** para gerenciar todos os microserviços em um monorepo, facilitando a manutenção e evolução da arquitetura.

## MVP - Fluxo de Negócio Principal

Como MVP para demonstrar todas as funcionalidades arquiteturais, implementaremos **um fluxo de negócio completo**:

### Fluxo de Exemplo (Processo de Pedido End-to-End)

1. **Cliente cria Pedido** via BFF → BFF chama **Order Service** (API única)
2. **Order Service** publica evento `OrderCreated` no Kafka
3. **Billing Service** consome `OrderCreated`, processa pagamento (idempotente) e publica:
    - `PaymentCompleted` (sucesso) ou `PaymentFailed` (falha)
4. **Inventory Service** consome `PaymentCompleted`, reserva estoque e publica `InventoryReserved`
5. **Saga Pattern** implementado (coreografia) para garantir consistência cross-service
6. **BFF** agrega view model centrado no cliente (consulta read-model) e expõe para front-end

### APIs Mínimas por Microserviço

Cada microserviço implementa **uma API principal** para demonstrar o fluxo:

- **order-service**: `POST /orders` - Criação de pedidos
- **billing-service**: `POST /payments` - Processamento via eventos
- **inventory-service**: `POST /reserve` - Reserva via eventos
- **bff**: `GET /customer/orders` - View agregada com fallback/cache

### Patterns Demonstrados

- ✅ **Event-Driven Architecture** (Kafka)
- ✅ **Saga Pattern** (Coreografia)
- ✅ **CQRS** (Read Models)
- ✅ **Idempotência** (Deduplicação)
- ✅ **Cache Strategy** (L1 + L2)
- ✅ **Resilience Patterns** (Circuit Breaker, Retry, Timeout)
- ✅ **Observabilidade Completa** (Metrics, Tracing, Logs)

## Tecnologias

- **Java 21** com features modernas
- **Spring Boot 3.5.5**
- **Maven** para build e gerenciamento de dependências
- **Apache Kafka** para mensageria
- **Redis** para cache distribuído
- **PostgreSQL** para persistência
- **Avro/JSON** para serialização de mensagens
- **Docker Compose** para ambiente local
- **Kubernetes + Helm** para deploy
- **Terraform** para Infrastructure as Code
- **Prometheus + Grafana** para observabilidade
- **Jaeger** para distributed tracing

## Arquitetura

O projeto segue uma arquitetura de microserviços com os seguintes serviços:

### Serviços
- **Order Service**: Gerenciamento de pedidos
- **Billing Service**: Processamento de pagamentos
- **Inventory Service**: Controle de estoque
- **BFF (Backend for Frontend)**: Agregação de dados para clientes
- **Shared Kernel**: Código compartilhado entre serviços

### Padrões Arquiteturais
- **Arquitetura Hexagonal** (Ports & Adapters)
- **CQRS** (Command Query Responsibility Segregation)
- **Saga Pattern** para consistência eventual
- **Event Sourcing** para auditoria
- **Cache Strategy** (L1: Caffeine, L2: Redis)

## Estrutura do Projeto

```
/
├── shared-kernel/              # Código compartilhado entre serviços
├── order-service/              # Gerenciamento de pedidos
├── billing-service/            # Processamento de pagamentos
├── inventory-service/          # Controle de estoque
├── bff/                        # Backend for Frontend
├── infra/
│   └── terraform/
├── charts/                     # Helm Charts
├── docs/                       # Documentação
│   ├── C4/                     # Diagramas de arquitetura
│   ├── ADRs/                   # Architecture Decision Records
│   └── runbook.md              # Procedimentos operacionais
├── docker-compose.yml
├── pom.xml                     # Multi-module Maven configuration
└── README.md
```

### Estrutura por Microserviço (Arquitetura Hexagonal)

Todos os microserviços seguem a mesma estrutura hexagonal. Exemplo do **Order Service**:

```
order-service/
├── pom.xml
└── src/main/java/com/ecommerce/order/
    ├── adapter/
    │   ├── in/
    │   │   ├── web/                # REST Controllers
    │   │   └── messaging/          # Kafka Consumers
    │   └── out/
    │       ├── persistence/        # JPA Repositories
    │       ├── messaging/          # Kafka Producers
    │       └── cache/              # Redis Adapters
    ├── application/
    │   ├── port/
    │   │   ├── in/                 # Use Case Interfaces
    │   │   └── out/                # Repository Interfaces
    │   └── service/                # Use Case Implementations
    ├── domain/
    │   ├── model/                  # Entities, Value Objects
    │   └── event/                  # Domain Events
    └── infrastructure/
        ├── config/                 # Spring Configuration
        └── exception/              # Exception Handlers
```

## Configuração do Ambiente

### Pré-requisitos

- Java 21
- Maven 3.9+
- Docker & Docker Compose
- Kubernetes (local: minikube/kind)
- Helm
- Terraform

### Estrutura de Arquivos de Configuração

Para execução completa do ambiente, crie a estrutura de diretórios:

```bash
mkdir -p infrastructure/prometheus/rules
mkdir -p infrastructure/grafana/provisioning  
mkdir -p infrastructure/grafana/dashboards
mkdir -p infrastructure/postgres/init
```

Arquivos de configuração incluídos:
- `infrastructure/prometheus/prometheus.yml` - Configuração de coleta de métricas
- `docker-compose.yml` - Stack completa de desenvolvimento

### Ambiente Local (Docker Compose)

1. Clone o repositório:
```bash
git clone <repository-url>
cd projeto-microservicos
```

2. Execute o ambiente local:
```bash
docker-compose up -d
```

Serviços disponíveis:
- **Kafka**: localhost:9092
- **Kafka UI**: http://localhost:8090
- **Redis**: localhost:6379
- **PostgreSQL**: localhost:5432
- **Jaeger**: http://localhost:16686
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin)
- **LocalStack**: http://localhost:4566

### Execução dos Microserviços

```bash
# Order Service
cd order-service
mvn spring-boot:run -Dserver.port=8081

# Billing Service
cd billing-service  
mvn spring-boot:run -Dserver.port=8082

# Inventory Service
cd inventory-service
mvn spring-boot:run -Dserver.port=8083

# BFF
cd bff
mvn spring-boot:run -Dserver.port=8084
```

**Nota**: O módulo `shared-kernel` é uma biblioteca compartilhada e não é executável.

## Deploy no Kubernetes

### 1. Provisionar Infraestrutura (Terraform)

```bash
cd infra/terraform
terraform init
terraform plan
terraform apply
```

### 2. Deploy com Helm

```bash
# Deploy de cada microserviço executável
helm install order-service charts/order-service -f charts/order-service/values-dev.yaml
helm install billing-service charts/billing-service -f charts/billing-service/values-dev.yaml
helm install inventory-service charts/inventory-service -f charts/inventory-service/values-dev.yaml
helm install bff charts/bff -f charts/bff/values-dev.yaml
```

## Testes

O projeto implementa testes unitários focados nas classes de service/use cases:

```bash
# Executar todos os testes do projeto (multi-module)
mvn test

# Executar testes de um serviço específico
cd order-service
mvn test

# Executar apenas testes de uma classe específica
mvn test -Dtest=OrderServiceTest
```

## Observabilidade

### Métricas Monitoradas
- **Latência**: p95/p99 por endpoint dos microserviços executáveis
- **Taxa de Erro**: success rate por serviço (Order, Billing, Inventory, BFF)
- **Throughput**: requests/segundo
- **Consumer Lag**: atraso de processamento Kafka
- **Cache Hit Rate**: eficácia do cache Redis
- **JVM Metrics**: heap, GC, threads por microserviço

### Health Checks
Todos os serviços implementam:
- `/actuator/health` - Liveness probe
- `/actuator/health/readiness` - Readiness probe

### Tracing
- Propagação de trace IDs entre serviços
- Correlação de logs por requestId/traceId

## Resiliência

### Padrões Implementados
- **Timeouts**: configurados por adapter
- **Retries**: com jitter exponential backoff
- **Circuit Breaker**: usando Resilience4j
- **Bulkhead**: isolamento de thread pools
- **Rate Limiting**: por consumidor

### Idempotência
- Store de chaves de deduplicação (Redis)
- Event IDs únicos para mensagens Kafka
- Headers de trace para correlação

## Mensageria (Kafka)

### Tópicos Principais
- `order.events` - Eventos de pedidos (`OrderCreated`, `OrderUpdated`)
- `billing.events` - Eventos de pagamento (`PaymentCompleted`, `PaymentFailed`)
- `inventory.events` - Eventos de estoque (`InventoryReserved`, `InventoryReleased`)
- `notification.events` - Notificações para cliente

### Estratégia de Particionamento
- Chave por ID do usuário/pedido
- Consumer groups por serviço
- Configuração de idempotência

## Cache Strategy

### L1 Cache (Caffeine)
- Cache in-process para consultas frequentes
- TTL configurável por tipo de dados
- Eviction baseada em tamanho e tempo

### L2 Cache (Redis)
- Cache distribuído para read models
- Invalidação por eventos de domínio
- Clustering para alta disponibilidade

## CI/CD

### Pipelines Configurados

#### Application Pipeline
1. Build → Unit Tests
2. Integration Tests (docker-compose)
3. Image Build & Push
4. Deploy to Staging (Helm)
5. Production Deploy (manual approval)

#### Infrastructure Pipeline
1. Terraform Format & Validate
2. Plan (comentário no PR)
3. Apply (após aprovação manual)

### Versionamento
- Semantic Versioning (SemVer)
- Git tags automáticos
- Changelog automatizado

## FinOps & Governança

### Tags Obrigatórias
```yaml
team: "ecommerce"
project: "microservices-platform"  
environment: "dev|staging|prod"
cost-center: "engineering"
```

### Monitoramento de Custos
- Budget alerts configurados
- Autoscaling HPA/KEDA
- Resource quotas por namespace

## Documentação

### Disponível em `/docs`
- **C4 Diagrams**: Context, Container, Component, Deployment
- **ADRs**: Decisões arquiteturais documentadas
- **Runbook**: Procedimentos operacionais
- **API Documentation**: OpenAPI/Swagger

## Runbook - Principais Cenários

### Alto Consumer Lag
1. Verificar health dos consumers: `kubectl get pods -l app=order-service`
2. Verificar logs: `kubectl logs -f deployment/order-service`
3. Escalar consumers: `kubectl scale deployment/order-service --replicas=3`
4. Monitorar recuperação do lag via Grafana

### Alta Latência (p95 > threshold)
1. Verificar tracing no Jaeger para identificar gargalos
2. Analisar cache hit rate no Grafana (L1 Caffeine + L2 Redis)
3. Verificar connection pools do PostgreSQL
4. Ativar circuit breaker se necessário via Resilience4j

### Falha de Pagamento (Saga Compensation)
1. Verificar logs do billing-service para errors de pagamento
2. Confirmar publicação de `PaymentFailed` no Kafka
3. Verificar compensação da Saga (liberação de estoque)
4. Ativar fallback no BFF para experiência do usuário
5. Processar retry manual se necessário via admin endpoint

## Contribuição

### Convenções
- **Conventional Commits** para mensagens de commit
- **GitFlow** para branching strategy
- **PR Template** obrigatório
- **Code Review** mandatório

### Qualidade de Código
- SonarQube analysis obrigatória
- Cobertura de testes > 80% para use cases
- Adherência aos princípios SOLID, DRY, YAGNI
- Uso de features modernas do Java 21

## Contato

Para dúvidas ou suporte, consulte a documentação em `/docs` ou abra uma issue no repositório.