# E-commerce Techbra - Plataforma de Microserviços

## Visão Geral

Este projeto implementa uma plataforma de e-commerce completa baseada em microserviços utilizando arquitetura hexagonal, seguindo as melhores práticas de desenvolvimento com SOLID, DRY e YAGNI.

A aplicação utiliza **Maven Multi-Module** para gerenciar todos os microserviços em um monorepo, facilitando a manutenção e evolução da arquitetura. O projeto está configurado para execução em ambiente Windows com PowerShell.

## MVP - Fluxo de Negócio Principal

Como MVP para demonstrar todas as funcionalidades arquiteturais, implementaremos **um fluxo de negócio completo**:

### Fluxo de Exemplo (Processo de Pedido End-to-End)

1. **Cliente cria Pedido** via BFF → BFF chama **Order Service**
2. **Order Service** publica evento `OrderCreated` no Kafka
3. **Billing Service** consome `OrderCreated`, processa pagamento e publica:
    - `PaymentCompleted` (sucesso) ou `PaymentFailed` (falha)
4. **Inventory Service** consome `PaymentCompleted`, reserva estoque e publica `InventoryReserved`
5. **TODO**: **Notification Service** consumirá eventos para enviar notificações aos clientes
6. **Saga Pattern** implementado para garantir consistência cross-service
7. **BFF** agrega dados de múltiplos serviços e expõe APIs otimizadas para o frontend

### APIs Implementadas por Microserviço

**Order Service** (Porta 8081):
- `POST /api/orders` - Criação de pedidos
- `GET /api/orders/{id}` - Consulta de pedido
- `GET /api/orders` - Listagem de pedidos

**Billing Service** (Porta 8082):
- `POST /api/payments` - Processamento de pagamentos
- `GET /api/payments/{orderId}` - Status do pagamento

**Inventory Service** (Porta 8083):
- `POST /api/inventory/reserve` - Reserva de estoque
- `GET /api/inventory/products/{productId}` - Consulta de estoque
- `PUT /api/inventory/products/{productId}` - Atualização de estoque

**BFF** (Porta 8084):
- `POST /api/orders` - Criação de pedidos (proxy para order-service)
- `GET /api/orders/{id}` - Detalhes do pedido com dados agregados
- `GET /api/customer/orders` - Histórico de pedidos do cliente

**TODO**: **Notification Service** - Será implementado futuramente para:
- Envio de emails de confirmação
- Notificações push
- SMS de status de pedidos

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
- **Maven 3.9+** para build e gerenciamento de dependências
- **Apache Kafka** para mensageria assíncrona
- **Redis** para cache distribuído
- **PostgreSQL** para persistência
- **JSON** para serialização de mensagens
- **Docker & Docker Compose** para containerização
- **Kubernetes (Kind)** para orquestração
- **Helm Charts** para deploy no Kubernetes
- **Prometheus + Grafana** para observabilidade
- **Jaeger** para distributed tracing
- **Resilience4j** para padrões de resiliência

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

### Pré-requisitos (Windows)

- **Java 21** (OpenJDK ou Oracle JDK)
- **Maven 3.9+**
- **Docker Desktop** para Windows
- **PowerShell 5.1+** (ou PowerShell Core 7+)
- **Kind** para Kubernetes local
- **Helm 3.x**
- **kubectl** para gerenciar Kubernetes

### Instalação dos Pré-requisitos

```powershell
# Instalar Chocolatey (se não tiver)
Set-ExecutionPolicy Bypass -Scope Process -Force; [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072; iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))

# Instalar ferramentas via Chocolatey
choco install openjdk21 maven docker-desktop kind helm kubectl

# Verificar instalações
java -version
mvn -version
docker --version
kind --version
helm version
kubectl version --client
```

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

1. **Clone o repositório:**
```powershell
git clone <repository-url>
cd ecommerce-techbra
```

2. **Inicie a infraestrutura:**
```powershell
# Subir toda a infraestrutura (Kafka, Redis, PostgreSQL, etc.)
docker-compose up -d

# Verificar se todos os serviços estão rodando
docker-compose ps

# Ver logs se necessário
docker-compose logs -f kafka
```

3. **Aguardar inicialização completa:**
```powershell
# Aguardar Kafka estar pronto (pode levar 1-2 minutos)
docker-compose logs kafka | Select-String "started"

# Verificar saúde dos serviços
Invoke-RestMethod -Uri "http://localhost:8090" # Kafka UI
Invoke-RestMethod -Uri "http://localhost:9090" # Prometheus
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

**Opção 1: Execução Individual (Desenvolvimento)**
```powershell
# Terminal 1 - Order Service
cd order-service
mvn spring-boot:run

# Terminal 2 - Billing Service
cd billing-service
mvn spring-boot:run

# Terminal 3 - Inventory Service
cd inventory-service
mvn spring-boot:run

# Terminal 4 - BFF
cd bff
mvn spring-boot:run
```

**Opção 2: Build e Execução via Docker**
```powershell
# Build de todas as imagens
.\deploy\build-images.ps1

# Ou build individual
docker build -f order-service/Dockerfile -t techbra/order-service:latest .
docker build -f billing-service/Dockerfile -t techbra/billing-service:latest .
docker build -f inventory-service/Dockerfile -t techbra/inventory-service:latest .
docker build -f bff/Dockerfile -t techbra/bff:latest .
```

**Opção 3: Deploy no Kubernetes Local**
```powershell
# Criar cluster Kind
kind create cluster --name techbra-ecommerce

# Carregar imagens no Kind
kind load docker-image techbra/order-service:latest --name techbra-ecommerce
kind load docker-image techbra/billing-service:latest --name techbra-ecommerce
kind load docker-image techbra/inventory-service:latest --name techbra-ecommerce
kind load docker-image techbra/bff:latest --name techbra-ecommerce

# Deploy com Helm
.\deploy\deploy-local.ps1
```

**Nota**: O módulo `shared-kernel` é uma biblioteca compartilhada e não é executável.

## Deploy no Kubernetes

### 1. Preparação do Ambiente Local

```powershell
# Criar cluster Kind
kind create cluster --name techbra-ecommerce --config deploy/kind-config.yaml

# Configurar kubectl
kubectl cluster-info --context kind-techbra-ecommerce

# Criar namespace
kubectl create namespace techbra-test
kubectl config set-context --current --namespace=techbra-test
```

### 2. Deploy da Infraestrutura

```powershell
# Deploy do Kafka
helm install kafka charts/infrastructure/kafka -n techbra-test

# Deploy do PostgreSQL
helm install postgres charts/infrastructure/postgres -n techbra-test

# Deploy do Redis
helm install redis charts/infrastructure/redis -n techbra-test

# Aguardar infraestrutura ficar pronta
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=kafka -n techbra-test --timeout=300s
```

### 3. Deploy dos Microserviços

```powershell
# Build e carregamento das imagens
.\deploy\build-images.ps1
.\deploy\load-images-kind.ps1

# Deploy com Helm
helm install order-service charts/order-service -f charts/order-service/values-dev.yaml -n techbra-test
helm install billing-service charts/billing-service -f charts/billing-service/values-dev.yaml -n techbra-test
helm install inventory-service charts/inventory-service -f charts/inventory-service/values-dev.yaml -n techbra-test
helm install bff charts/bff -f charts/bff/values-dev.yaml -n techbra-test

# Verificar deployments
kubectl get pods -n techbra-test
kubectl get services -n techbra-test
```

### 4. Acesso aos Serviços

```powershell
# Port-forward para acessar localmente
kubectl port-forward service/bff 8084:8084 -n techbra-test
kubectl port-forward service/order-service 8081:8081 -n techbra-test
kubectl port-forward service/kafka-ui 8090:8080 -n techbra-test

# Testar APIs
Invoke-RestMethod -Uri "http://localhost:8084/actuator/health"
Invoke-RestMethod -Uri "http://localhost:8081/actuator/health"
```

**Nota**: Para deploy em produção com Terraform, consulte a documentação específica em `/docs/deployment.md` (TODO: será criada futuramente).

## Testes

O projeto implementa testes unitários e de integração:

```powershell
# Executar todos os testes do projeto (multi-module)
mvn test

# Executar testes de um serviço específico
cd order-service
mvn test

# Executar apenas testes de uma classe específica
mvn test -Dtest=OrderServiceTest

# Executar testes com relatório de cobertura
mvn test jacoco:report
```

### Estrutura de Testes
- **Testes Unitários**: Focados em use cases e domain logic dos serviços

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
- `order.events` - Eventos de pedidos (`OrderCreated`, `OrderUpdated`, `OrderCancelled`)
- `billing.events` - Eventos de pagamento (`PaymentCompleted`, `PaymentFailed`, `PaymentRefunded`)
- `inventory.events` - Eventos de estoque (`InventoryReserved`, `InventoryReleased`, `StockUpdated`)
- `notification.events` - **TODO**: Notificações para cliente (será implementado futuramente)

### Monitoramento Kafka
```powershell
# Acessar Kafka UI
Start-Process "http://localhost:8090"

# Verificar tópicos via CLI (dentro do container)
docker exec -it ecommerce-techbra-kafka-1 kafka-topics --bootstrap-server localhost:9092 --list

# Consumir mensagens para debug
docker exec -it ecommerce-techbra-kafka-1 kafka-console-consumer --bootstrap-server localhost:9092 --topic order.events --from-beginning
```

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

## Troubleshooting

### Problemas Comuns

**1. Serviços não conseguem conectar ao Kafka**
```powershell
# Verificar se Kafka está rodando
docker-compose ps kafka

# Verificar logs do Kafka
docker-compose logs kafka

# Testar conectividade
telnet localhost 9092
```

**2. Erro de conexão com PostgreSQL**
```powershell
# Verificar se PostgreSQL está rodando
docker-compose ps postgres

# Conectar ao banco para testar
docker exec -it ecommerce-techbra-postgres-1 psql -U ecommerce_user -d ecommerce_db
```

**3. Alto Consumer Lag no Kubernetes**
```powershell
# Verificar pods
kubectl get pods -l app.kubernetes.io/name=order-service -n techbra-test

# Verificar logs
kubectl logs -f deployment/order-service -n techbra-test

# Escalar consumers
kubectl scale deployment/order-service --replicas=3 -n techbra-test
```

**4. Falha de Pagamento (Saga)**
```powershell
# Verificar logs do billing-service
kubectl logs -f deployment/billing-service -n techbra-test

# Verificar tópicos Kafka
kubectl port-forward service/kafka-ui 8090:8080 -n techbra-test
# Acessar http://localhost:8090
```

### Scripts de Diagnóstico
```powershell
# Health check completo
.\deploy\health-check.ps1

# Limpeza do ambiente
.\deploy\cleanup.ps1

# Restart completo
.\deploy\restart-all.ps1
```

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

## Scripts Úteis

O projeto inclui scripts PowerShell para facilitar operações comuns:

```powershell
# Build de todas as imagens Docker
.\deploy\build-images.ps1

# Deploy completo no Kubernetes local
.\deploy\deploy-local.ps1

# Teste end-to-end do fluxo de pedidos
.\deploy\test-order-service.ps1

# Health check de todos os serviços
.\deploy\health-check.ps1

# Limpeza completa do ambiente
.\deploy\cleanup.ps1
```

## Runbook - Principais Cenários

### Alto Consumer Lag
1. Verificar health dos consumers:
```powershell
kubectl get pods -l app.kubernetes.io/name=order-service -n techbra-test
```
2. Verificar logs:
```powershell
kubectl logs -f deployment/order-service -n techbra-test
```
3. Escalar consumers:
```powershell
kubectl scale deployment/order-service --replicas=3 -n techbra-test
```
4. Monitorar recuperação do lag via Grafana

### Alta Latência (p95 > threshold)
1. Verificar tracing no Jaeger para identificar gargalos
2. Analisar cache hit rate no Grafana (L1 Caffeine + L2 Redis)
3. Verificar connection pools do PostgreSQL
4. Ativar circuit breaker se necessário via Resilience4j

### Falha de Pagamento (Saga Compensation)
1. Verificar logs do billing-service para errors de pagamento:
```powershell
kubectl logs -f deployment/billing-service -n techbra-test
```
2. Confirmar publicação de `PaymentFailed` no Kafka via Kafka UI
3. Verificar compensação da Saga (liberação de estoque)
4. Ativar fallback no BFF para experiência do usuário
5. Processar retry manual se necessário via admin endpoint

### Falha de Conectividade com Kafka
1. Verificar status do cluster Kafka:
```powershell
kubectl get pods -l app.kubernetes.io/name=kafka -n techbra-test
```
2. Verificar logs do Kafka:
```powershell
kubectl logs -f deployment/kafka -n techbra-test
```
3. Testar conectividade dos serviços:
```powershell
kubectl exec -it deployment/order-service -n techbra-test -- telnet kafka-kafka 9092
```
4. Reiniciar serviços se necessário

### Degradação de Performance do PostgreSQL
1. Verificar métricas de CPU/Memory do PostgreSQL
2. Analisar slow queries:
```sql
SELECT query, mean_time, calls FROM pg_stat_statements ORDER BY mean_time DESC LIMIT 10;
```
3. Verificar connection pool leaks
4. Considerar read replicas se necessário

## Contato

Para dúvidas ou suporte:
- Consulte a documentação em `/docs`
- Abra uma issue no repositório
- Verifique os logs com os scripts de diagnóstico

---

**Nota**: Este projeto está em desenvolvimento ativo. Consulte o roadmap acima para funcionalidades planejadas.