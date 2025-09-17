# ADR-001: Adoção de Event-Driven Architecture com Apache Kafka

## Status
**Aceita** - 2024-01-15

## Contexto

O sistema de e-commerce Techbra precisa de uma arquitetura que permita:
- Comunicação assíncrona entre microserviços
- Desacoplamento temporal entre serviços
- Garantia de entrega de mensagens
- Capacidade de replay de eventos
- Escalabilidade horizontal
- Resiliência a falhas de componentes individuais

As opções consideradas foram:
1. **Comunicação síncrona** apenas (REST)
2. **Message Queues tradicionais** (RabbitMQ)
3. **Event Streaming Platform** (Apache Kafka)
4. **Cloud Messaging** (AWS SQS/SNS)

## Decisão

Adotamos **Apache Kafka** como plataforma de event streaming para implementar Event-Driven Architecture.

### Justificativa

**Vantagens do Kafka:**
- **Durabilidade**: Eventos persistidos em disco com replicação
- **Throughput**: Alta performance para grandes volumes
- **Replay**: Capacidade de reprocessar eventos históricos
- **Particionamento**: Escalabilidade horizontal automática
- **Ecosystem**: Integração com Spring Boot via Spring Kafka
- **Observabilidade**: Métricas nativas e ferramentas de monitoramento

**Comparação com alternativas:**

| Critério | REST | RabbitMQ | Kafka | AWS SQS |
|----------|------|----------|-------|----------|
| Throughput | Baixo | Médio | Alto | Médio |
| Durabilidade | N/A | Limitada | Alta | Alta |
| Replay | N/A | Não | Sim | Limitado |
| Complexidade | Baixa | Média | Alta | Baixa |
| Vendor Lock-in | N/A | Não | Não | Sim |
| Custo | Baixo | Médio | Médio | Variável |

## Implementação

### Tópicos Kafka

```yaml
Tópicos:
  order-events:
    partitions: 3
    replication-factor: 2
    events: [OrderCreated, OrderCancelled, OrderCompleted]
    
  payment-events:
    partitions: 3
    replication-factor: 2
    events: [PaymentProcessed, PaymentFailed, PaymentRefunded]
    
  inventory-events:
    partitions: 3
    replication-factor: 2
    events: [StockReserved, StockReleased, StockUpdated]
```

### Padrões de Eventos

```json
{
  "eventId": "uuid",
  "eventType": "OrderCreated",
  "aggregateId": "order-123",
  "occurredAt": "2024-01-15T10:30:00Z",
  "version": 1,
  "data": {
    "orderId": 123,
    "customerId": 456,
    "items": [...],
    "totalAmount": 299.99
  }
}
```

### Configuração Spring Kafka

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 3
    consumer:
      group-id: ${spring.application.name}
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      auto-offset-reset: earliest
      enable-auto-commit: false
```

## Consequências

### Positivas

✅ **Desacoplamento**: Serviços não precisam conhecer uns aos outros diretamente

✅ **Escalabilidade**: Cada serviço pode escalar independentemente

✅ **Resiliência**: Falha de um serviço não afeta outros diretamente

✅ **Auditoria**: Histórico completo de eventos do sistema

✅ **Flexibilidade**: Novos consumidores podem ser adicionados sem modificar produtores

✅ **Performance**: Comunicação assíncrona melhora responsividade

### Negativas

❌ **Complexidade**: Maior complexidade operacional e de desenvolvimento

❌ **Consistência Eventual**: Dados podem estar temporariamente inconsistentes

❌ **Debugging**: Mais difícil rastrear fluxos end-to-end

❌ **Infraestrutura**: Necessidade de gerenciar cluster Kafka

❌ **Latência**: Pequena latência adicional vs comunicação síncrona

❌ **Duplicação**: Necessidade de lidar com mensagens duplicadas

## Mitigações

### Complexidade
- Uso de Spring Kafka para abstrair complexidade
- Documentação detalhada de eventos e fluxos
- Ferramentas de monitoramento (Kafka UI, Prometheus)

### Consistência Eventual
- Implementação de Saga Pattern para transações distribuídas
- Timeouts e compensação para operações críticas
- Monitoramento de lag de consumidores

### Debugging
- Correlation IDs em todos os eventos
- Distributed tracing com Jaeger
- Logs estruturados com contexto de eventos

### Duplicação
- Implementação de idempotência em todos os consumidores
- Uso de chaves de deduplicação
- Versionamento de eventos para evolução

## Métricas de Sucesso

- **Throughput**: > 1000 eventos/segundo
- **Latência**: < 100ms para processamento de eventos
- **Disponibilidade**: 99.9% uptime do cluster Kafka
- **Consumer Lag**: < 1000 mensagens em condições normais
- **Reprocessamento**: Capacidade de replay de 30 dias de eventos

## Revisão

Esta decisão deve ser revisada em:
- **6 meses**: Avaliação de métricas e complexidade operacional
- **1 ano**: Consideração de alternativas emergentes
- **Quando**: Problemas significativos de performance ou operação

## Referências

- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Spring Kafka Reference](https://docs.spring.io/spring-kafka/docs/current/reference/html/)
- [Event-Driven Architecture Patterns](https://microservices.io/patterns/data/event-driven-architecture.html)
- [Saga Pattern](https://microservices.io/patterns/data/saga.html)