# Runbook - E-commerce Techbra

## Visão Geral

Este runbook contém procedimentos operacionais para monitoramento, troubleshooting e manutenção da plataforma de e-commerce Techbra em ambiente de produção e desenvolvimento.

## Índice

1. [Monitoramento e Alertas](#monitoramento-e-alertas)
2. [Cenários de Troubleshooting](#cenários-de-troubleshooting)
3. [Procedimentos de Manutenção](#procedimentos-de-manutenção)
4. [Escalação e Contatos](#escalação-e-contatos)
5. [Scripts de Automação](#scripts-de-automação)

## Monitoramento e Alertas

### Métricas Críticas

#### Kafka
- **Consumer Lag**: < 1000 mensagens
- **Throughput**: Monitorar msgs/sec por tópico
- **Disk Usage**: < 80%
- **Memory Usage**: < 85%

#### Microserviços
- **Response Time (p95)**: < 500ms
- **Error Rate**: < 1%
- **CPU Usage**: < 70%
- **Memory Usage**: < 80%
- **JVM Heap**: < 75%

#### PostgreSQL
- **Connection Pool**: < 80% utilização
- **Query Performance**: < 100ms (p95)
- **Disk I/O**: Monitorar IOPS
- **Lock Waits**: < 5%

#### Redis
- **Memory Usage**: < 80%
- **Cache Hit Rate**: > 95%
- **Connection Count**: Monitorar picos

### Dashboards Grafana

```powershell
# Acessar Grafana
kubectl port-forward service/grafana 3000:3000 -n techbra-test
# http://localhost:3000 (admin/admin)
```

**Dashboards Principais:**
- **Application Overview**: Métricas gerais dos microserviços
- **Kafka Monitoring**: Consumer lag, throughput, partições
- **Database Performance**: PostgreSQL queries, connections
- **Infrastructure**: CPU, Memory, Disk, Network

## Cenários de Troubleshooting

### 1. Alto Consumer Lag no Kafka

**Sintomas:**
- Consumer lag > 1000 mensagens
- Processamento lento de eventos
- Timeout em operações assíncronas

**Diagnóstico:**
```powershell
# Verificar consumer groups
kubectl exec -it deployment/kafka -n techbra-test -- kafka-consumer-groups --bootstrap-server localhost:9092 --list

# Verificar lag específico
kubectl exec -it deployment/kafka -n techbra-test -- kafka-consumer-groups --bootstrap-server localhost:9092 --group order-service-group --describe

# Verificar health dos consumers
kubectl get pods -l app.kubernetes.io/name=order-service -n techbra-test
kubectl logs -f deployment/order-service -n techbra-test | Select-String "ERROR|WARN"
```

**Resolução:**
```powershell
# 1. Escalar consumers horizontalmente
kubectl scale deployment/order-service --replicas=3 -n techbra-test
kubectl scale deployment/billing-service --replicas=2 -n techbra-test

# 2. Verificar se há dead letter queue
kubectl exec -it deployment/kafka -n techbra-test -- kafka-topics --bootstrap-server localhost:9092 --list | Select-String "dlq"

# 3. Monitorar recuperação
kubectl exec -it deployment/kafka -n techbra-test -- kafka-consumer-groups --bootstrap-server localhost:9092 --group order-service-group --describe
```

**Prevenção:**
- Configurar auto-scaling baseado em consumer lag
- Implementar circuit breaker nos consumers
- Monitorar padrões de tráfego

### 2. Alta Latência de API (p95 > 500ms)

**Sintomas:**
- Response time elevado
- Timeout em requests
- Degradação da experiência do usuário

**Diagnóstico:**
```powershell
# Verificar métricas de latência
Invoke-RestMethod -Uri "http://localhost:8084/actuator/metrics/http.server.requests" | ConvertTo-Json

# Verificar tracing no Jaeger
kubectl port-forward service/jaeger 16686:16686 -n techbra-test
# http://localhost:16686

# Verificar logs de performance
kubectl logs -f deployment/bff -n techbra-test | Select-String "slow|timeout|latency"
```

**Resolução:**
```powershell
# 1. Verificar cache hit rate
kubectl exec -it deployment/redis -n techbra-test -- redis-cli info stats | Select-String "hit"

# 2. Analisar connection pools
kubectl logs -f deployment/order-service -n techbra-test | Select-String "pool|connection"

# 3. Ativar circuit breaker se necessário
# Verificar configuração Resilience4j nos logs
kubectl logs -f deployment/bff -n techbra-test | Select-String "circuit|breaker"

# 4. Escalar serviços se necessário
kubectl scale deployment/bff --replicas=3 -n techbra-test
```

**Prevenção:**
- Configurar cache adequadamente
- Implementar connection pooling otimizado
- Usar circuit breakers e timeouts
- Monitorar dependency health

### 3. Falha de Pagamento (Saga Compensation)

**Sintomas:**
- Pedidos ficam em estado inconsistente
- Eventos `PaymentFailed` não processados
- Estoque não liberado após falha

**Diagnóstico:**
```powershell
# Verificar logs do billing-service
kubectl logs -f deployment/billing-service -n techbra-test | Select-String "payment|error|failed"

# Verificar eventos no Kafka
kubectl port-forward service/kafka-ui 8090:8080 -n techbra-test
# Acessar http://localhost:8090 e verificar tópico billing.events

# Verificar estado dos pedidos
Invoke-RestMethod -Uri "http://localhost:8081/api/orders" | ConvertTo-Json
```

**Resolução:**
```powershell
# 1. Verificar publicação de eventos de compensação
kubectl exec -it deployment/kafka -n techbra-test -- kafka-console-consumer --bootstrap-server localhost:9092 --topic billing.events --from-beginning | Select-String "PaymentFailed"

# 2. Verificar se inventory-service processou compensação
kubectl logs -f deployment/inventory-service -n techbra-test | Select-String "compensation|rollback|release"

# 3. Processar retry manual se necessário
# POST /api/payments/{orderId}/retry (endpoint admin)
Invoke-RestMethod -Uri "http://localhost:8082/api/payments/ORDER123/retry" -Method POST

# Verificar fallback no BFF
kubectl logs -f deployment/bff -n techbra-test | Select-String "fallback|circuit"
```

**Prevenção:**
- Implementar idempotência em todos os handlers
- Configurar dead letter queues
- Monitorar saga timeouts
- Implementar compensação automática

### 4. Falha de Conectividade com Kafka

**Sintomas:**
- Serviços não conseguem publicar/consumir eventos
- Erros de connection timeout
- Degradação do fluxo assíncrono

**Diagnóstico:**
```powershell
# Verificar status do cluster Kafka
kubectl get pods -l app.kubernetes.io/name=kafka -n techbra-test
kubectl describe pod kafka-0 -n techbra-test

# Verificar logs do Kafka
kubectl logs -f kafka-0 -n techbra-test | Select-String "ERROR|WARN"

# Testar conectividade dos serviços
kubectl exec -it deployment/order-service -n techbra-test -- telnet kafka-kafka 9092
```

**Resolução:**
```powershell
# 1. Reiniciar Kafka se necessário
kubectl rollout restart statefulset/kafka -n techbra-test
kubectl rollout status statefulset/kafka -n techbra-test

# 2. Verificar configuração de rede
kubectl get svc kafka-kafka -n techbra-test
kubectl describe svc kafka-kafka -n techbra-test

# 3. Reiniciar serviços consumidores
kubectl rollout restart deployment/order-service -n techbra-test
kubectl rollout restart deployment/billing-service -n techbra-test
kubectl rollout restart deployment/inventory-service -n techbra-test

# 4. Verificar recuperação
kubectl logs -f deployment/order-service -n techbra-test | Select-String "kafka|connected"
```

### 5. Degradação de Performance do PostgreSQL

**Sintomas:**
- Queries lentas (> 100ms)
- Connection pool exhaustion
- Lock waits elevados

**Diagnóstico:**
```powershell
# Conectar ao PostgreSQL
kubectl exec -it deployment/postgres -n techbra-test -- psql -U ecommerce_user -d ecommerce_db

# Verificar slow queries
SELECT query, mean_time, calls, total_time 
FROM pg_stat_statements 
ORDER BY mean_time DESC 
LIMIT 10;

# Verificar locks
SELECT * FROM pg_locks WHERE NOT granted;

# Verificar conexões ativas
SELECT count(*) FROM pg_stat_activity WHERE state = 'active';
```

**Resolução:**
```powershell
# 1. Analisar e otimizar queries lentas
# Verificar índices necessários
SELECT schemaname, tablename, indexname, idx_tup_read, idx_tup_fetch 
FROM pg_stat_user_indexes 
ORDER BY idx_tup_read DESC;

# 2. Verificar connection pool leaks
kubectl logs -f deployment/order-service -n techbra-test | Select-String "pool|connection|leak"

# 3. Reiniciar connection pools se necessário
kubectl rollout restart deployment/order-service -n techbra-test

# 4. Considerar read replicas para queries de leitura
# (Implementação futura)
```

## Procedimentos de Manutenção

### Deploy de Nova Versão

```powershell
# 1. Build da nova imagem
docker build -f order-service/Dockerfile -t techbra/order-service:v1.2.0 .

# 2. Carregar no Kind (desenvolvimento)
kind load docker-image techbra/order-service:v1.2.0 --name techbra-ecommerce

# 3. Atualizar Helm chart
helm upgrade order-service charts/order-service --set image.tag=v1.2.0 -n techbra-test

# 4. Verificar rollout
kubectl rollout status deployment/order-service -n techbra-test

# Verificar health
Invoke-RestMethod -Uri "http://localhost:8081/actuator/health"

# 6. Rollback se necessário
kubectl rollout undo deployment/order-service -n techbra-test
```

### Backup e Restore

```powershell
# Backup PostgreSQL
kubectl exec -it deployment/postgres -n techbra-test -- pg_dump -U ecommerce_user ecommerce_db > backup_$(Get-Date -Format "yyyyMMdd_HHmmss").sql

# Restore PostgreSQL
kubectl exec -i deployment/postgres -n techbra-test -- psql -U ecommerce_user ecommerce_db < backup_20241201_120000.sql

# Backup Redis (se necessário)
kubectl exec -it deployment/redis -n techbra-test -- redis-cli BGSAVE
```

### Limpeza de Logs e Dados

```powershell
# Limpeza de logs antigos do Kafka
kubectl exec -it deployment/kafka -n techbra-test -- kafka-configs --bootstrap-server localhost:9092 --entity-type topics --entity-name order.events --alter --add-config retention.ms=604800000

# Limpeza de métricas antigas do Prometheus
# (Configurar retention no prometheus.yml)

# Limpeza de traces antigos do Jaeger
# (Configurar TTL no Jaeger)
```

## Scripts de Automação

### Health Check Completo

```powershell
# Script: health-check-complete.ps1

# Verificar todos os pods
$pods = kubectl get pods -n techbra-test -o json | ConvertFrom-Json
foreach ($pod in $pods.items) {
    if ($pod.status.phase -ne "Running") {
        Write-Host "ALERT: Pod $($pod.metadata.name) não está Running" -ForegroundColor Red
    }
}

# Verificar APIs
$services = @(
    @{Name="BFF"; Url="http://localhost:8084/actuator/health"},
    @{Name="Order"; Url="http://localhost:8081/actuator/health"},
    @{Name="Billing"; Url="http://localhost:8082/actuator/health"},
    @{Name="Inventory"; Url="http://localhost:8083/actuator/health"}
)

foreach ($service in $services) {
    try {
        $response = Invoke-RestMethod -Uri $service.Url -TimeoutSec 5
        if ($response.status -eq "UP") {
            Write-Host "OK: $($service.Name) está saudável" -ForegroundColor Green
        } else {
            Write-Host "WARN: $($service.Name) não está UP" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "ERROR: $($service.Name) não responde" -ForegroundColor Red
    }
}

# Verificar consumer lag
$lag = kubectl exec -it deployment/kafka -n techbra-test -- kafka-consumer-groups --bootstrap-server localhost:9092 --group order-service-group --describe
if ($lag -match "LAG.*[1-9][0-9]{3,}") {
    Write-Host "ALERT: Alto consumer lag detectado" -ForegroundColor Red
}
```

### Restart Automático

```powershell
# Script: auto-restart.ps1

# Reiniciar todos os microserviços
$deployments = @("order-service", "billing-service", "inventory-service", "bff")

foreach ($deployment in $deployments) {
    Write-Host "Reiniciando $deployment..."
    kubectl rollout restart deployment/$deployment -n techbra-test
    kubectl rollout status deployment/$deployment -n techbra-test --timeout=300s
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "$deployment reiniciado com sucesso" -ForegroundColor Green
    } else {
        Write-Host "Falha ao reiniciar $deployment" -ForegroundColor Red
    }
}
```

## Escalação e Contatos

### Níveis de Severidade

**Crítico (P1):**
- Sistema completamente indisponível
- Perda de dados
- Falha de segurança

**Alto (P2):**
- Funcionalidade principal degradada
- Performance severamente impactada
- Falha de componente crítico

**Médio (P3):**
- Funcionalidade secundária afetada
- Performance levemente degradada
- Warnings em componentes

**Baixo (P4):**
- Problemas cosméticos
- Melhorias de performance
- Documentação

### Procedimento de Escalação

1. **P1/P2**: Notificar imediatamente o time de desenvolvimento
2. **P3**: Criar ticket e notificar em até 2 horas
3. **P4**: Criar ticket para próximo sprint

### Contatos de Emergência

- **Tech Lead**: [email/telefone]
- **DevOps**: [email/telefone]
- **Product Owner**: [email/telefone]
- **Infraestrutura**: [email/telefone]

---

**Última atualização**: $(Get-Date -Format "dd/MM/yyyy HH:mm")
**Versão**: 1.0
**Responsável**: Time Techbra