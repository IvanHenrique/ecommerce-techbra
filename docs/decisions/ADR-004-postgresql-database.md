# ADR-004: PostgreSQL como Banco de Dados Principal

## Status
**Aceita** - 2024-01-15

## Contexto

O sistema e-commerce Techbra precisa de um sistema de gerenciamento de banco de dados que atenda aos seguintes requisitos:

- **Consistência ACID**: Transações confiáveis para operações financeiras
- **Escalabilidade**: Suporte a crescimento de dados e usuários
- **Performance**: Consultas rápidas e eficientes
- **Flexibilidade**: Suporte a diferentes tipos de dados
- **Maturidade**: Estabilidade e confiabilidade comprovadas
- **Observabilidade**: Métricas e monitoramento
- **Backup e Recovery**: Estratégias robustas de backup
- **Custo**: Solução economicamente viável

As opções consideradas foram:

1. **PostgreSQL** (Relacional)
2. **MySQL** (Relacional)
3. **MongoDB** (NoSQL - Document)
4. **Amazon RDS** (Managed PostgreSQL/MySQL)
5. **Amazon DynamoDB** (NoSQL - Key-Value)
6. **Oracle Database** (Relacional)

## Decisão

Adotamos **PostgreSQL 15+** como sistema de gerenciamento de banco de dados principal para todos os microserviços do sistema e-commerce Techbra.

### Configuração Base

```yaml
# docker-compose.yml
postgresql:
  image: postgres:15-alpine
  environment:
    POSTGRES_DB: ecommerce
    POSTGRES_USER: postgres
    POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    POSTGRES_INITDB_ARGS: "--encoding=UTF8 --locale=C"
  ports:
    - "5432:5432"
  volumes:
    - postgres_data:/var/lib/postgresql/data
    - ./init-scripts:/docker-entrypoint-initdb.d
  command: |
    postgres
    -c max_connections=200
    -c shared_buffers=256MB
    -c effective_cache_size=1GB
    -c maintenance_work_mem=64MB
    -c checkpoint_completion_target=0.9
    -c wal_buffers=16MB
    -c default_statistics_target=100
```

## Justificativa da Decisão

### Comparação Detalhada

| Critério | PostgreSQL | MySQL | MongoDB | RDS | DynamoDB | Oracle |
|----------|------------|-------|---------|-----|----------|--------|
| **ACID Compliance** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐⭐⭐ |
| **Performance** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **Escalabilidade** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **Flexibilidade** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ |
| **Custo** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ | ⭐ |
| **Maturidade** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **Comunidade** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ |
| **Operação** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐ |
| **Vendor Lock-in** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐ | ⭐ |

### Vantagens do PostgreSQL

✅ **ACID Compliance Completa**
- Transações totalmente ACID
- Isolamento de transações configurável
- Consistência garantida mesmo com falhas

✅ **Tipos de Dados Avançados**
- JSON/JSONB para dados semi-estruturados
- Arrays nativos
- Tipos geométricos e geográficos
- Tipos customizados

✅ **Performance Superior**
- Query planner avançado
- Índices especializados (B-tree, Hash, GiST, SP-GiST, GIN, BRIN)
- Parallel queries
- Partitioning nativo

✅ **Extensibilidade**
- Extensões como PostGIS, pg_stat_statements
- Stored procedures em múltiplas linguagens
- Custom functions e operators

✅ **Conformidade com Padrões**
- SQL:2016 compliance
- Portabilidade entre ambientes
- Padrões da indústria

✅ **Observabilidade**
- pg_stat_* views para monitoramento
- Query logging detalhado
- Métricas de performance integradas

## Implementação

### Estrutura de Bancos por Microserviço

```sql
-- Database per Service Pattern
CREATE DATABASE order_db;
CREATE DATABASE billing_db;
CREATE DATABASE inventory_db;

-- Usuários específicos por serviço
CREATE USER order_service WITH PASSWORD 'order_password';
CREATE USER billing_service WITH PASSWORD 'billing_password';
CREATE USER inventory_service WITH PASSWORD 'inventory_password';

-- Permissões granulares
GRANT ALL PRIVILEGES ON DATABASE order_db TO order_service;
GRANT ALL PRIVILEGES ON DATABASE billing_db TO billing_service;
GRANT ALL PRIVILEGES ON DATABASE inventory_db TO inventory_service;
```

### Schema do Order Service

```sql
-- orders table
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_amount DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    version INTEGER DEFAULT 0, -- Optimistic locking
    
    CONSTRAINT orders_status_check CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED')),
    CONSTRAINT orders_total_amount_check CHECK (total_amount >= 0)
);

-- order_items table
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    total_price DECIMAL(10,2) NOT NULL,
    
    CONSTRAINT order_items_quantity_check CHECK (quantity > 0),
    CONSTRAINT order_items_unit_price_check CHECK (unit_price >= 0),
    CONSTRAINT order_items_total_price_check CHECK (total_price >= 0)
);

-- Índices para performance
CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);

-- Trigger para updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_orders_updated_at
    BEFORE UPDATE ON orders
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

### Schema do Billing Service

```sql
-- payments table
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'BRL',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(50) NOT NULL,
    gateway_transaction_id VARCHAR(255),
    gateway_response JSONB, -- Flexibilidade para diferentes gateways
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT payments_status_check CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'REFUNDED')),
    CONSTRAINT payments_amount_check CHECK (amount > 0)
);

-- transactions table (para auditoria)
CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL REFERENCES payments(id),
    type VARCHAR(20) NOT NULL, -- CHARGE, REFUND, CHARGEBACK
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    gateway_response JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT transactions_type_check CHECK (type IN ('CHARGE', 'REFUND', 'CHARGEBACK'))
);

-- Índices
CREATE INDEX idx_payments_order_id ON payments(order_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_created_at ON payments(created_at);
CREATE INDEX idx_transactions_payment_id ON transactions(payment_id);

-- Índice GIN para consultas em JSONB
CREATE INDEX idx_payments_gateway_response ON payments USING GIN (gateway_response);
```

### Schema do Inventory Service

```sql
-- products table
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(100) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    category_id BIGINT,
    attributes JSONB, -- Atributos flexíveis do produto
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT products_price_check CHECK (price >= 0)
);

-- stock table
CREATE TABLE stock (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id),
    available_quantity INTEGER NOT NULL DEFAULT 0,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    total_quantity INTEGER GENERATED ALWAYS AS (available_quantity + reserved_quantity) STORED,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT stock_available_quantity_check CHECK (available_quantity >= 0),
    CONSTRAINT stock_reserved_quantity_check CHECK (reserved_quantity >= 0),
    
    UNIQUE(product_id)
);

-- stock_reservations table
CREATE TABLE stock_reservations (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id),
    order_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT stock_reservations_quantity_check CHECK (quantity > 0),
    CONSTRAINT stock_reservations_status_check CHECK (status IN ('ACTIVE', 'CONFIRMED', 'RELEASED', 'EXPIRED'))
);

-- Índices
CREATE INDEX idx_products_sku ON products(sku);
CREATE INDEX idx_products_category_id ON products(category_id);
CREATE INDEX idx_stock_product_id ON stock(product_id);
CREATE INDEX idx_stock_reservations_product_id ON stock_reservations(product_id);
CREATE INDEX idx_stock_reservations_order_id ON stock_reservations(order_id);
CREATE INDEX idx_stock_reservations_expires_at ON stock_reservations(expires_at);

-- Índice GIN para atributos JSONB
CREATE INDEX idx_products_attributes ON products USING GIN (attributes);
```

### Configuração de Connection Pool

**application.yml**
```yaml
spring:
  datasource:
    hikari:
      # Pool sizing
      maximum-pool-size: 20
      minimum-idle: 5
      
      # Connection timeout
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      
      # Performance
      leak-detection-threshold: 60000
      
      # Health check
      connection-test-query: SELECT 1
      validation-timeout: 5000
      
      # Pool name for monitoring
      pool-name: ${spring.application.name}-pool
```

### Migrations com Flyway

**V1__Create_orders_schema.sql**
```sql
-- Migration para Order Service
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_amount DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    version INTEGER DEFAULT 0
);

CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    total_price DECIMAL(10,2) NOT NULL
);

-- Índices iniciais
CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
```

## Consequências

### Positivas

✅ **Consistência de Dados**
- Transações ACID garantem integridade
- Foreign keys mantêm referential integrity
- Constraints validam dados na entrada

✅ **Performance Otimizada**
- Query planner inteligente
- Múltiplos tipos de índices
- Parallel processing
- Partitioning para grandes tabelas

✅ **Flexibilidade de Dados**
- Suporte a JSON/JSONB para dados semi-estruturados
- Arrays nativos
- Full-text search integrado
- Extensões especializadas

✅ **Observabilidade**
- pg_stat_statements para análise de queries
- Logs detalhados de performance
- Métricas integradas com Prometheus

✅ **Backup e Recovery**
- Point-in-time recovery
- Streaming replication
- Logical replication
- pg_dump/pg_restore

✅ **Custo-Benefício**
- Open source sem licenças
- Comunidade ativa
- Suporte comercial disponível

### Negativas

❌ **Complexidade Operacional**
- Configuração e tuning requerem expertise
- Monitoramento de múltiplas instâncias
- Backup e recovery procedures

❌ **Escalabilidade Horizontal**
- Sharding manual complexo
- Read replicas para scale-out de leitura
- Não é naturalmente distribuído

❌ **Memory Usage**
- Shared buffers e work_mem consomem RAM
- Connection overhead
- Cache warming após restarts

❌ **Lock Contention**
- Locks podem impactar concorrência
- Deadlocks em transações complexas
- Row-level locking overhead

## Mitigações

### Escalabilidade

**Read Replicas**
```yaml
# docker-compose.yml
postgresql-replica:
  image: postgres:15-alpine
  environment:
    POSTGRES_USER: replica
    POSTGRES_PASSWORD: ${REPLICA_PASSWORD}
    PGUSER: postgres
    POSTGRES_DB: ecommerce
    POSTGRES_MASTER_SERVICE: postgresql
  command: |
    bash -c '
    until pg_isready -h postgresql -p 5432; do
      echo "Waiting for master to be ready..."
      sleep 2
    done
    
    pg_basebackup -h postgresql -D /var/lib/postgresql/data -U replica -v -P -W
    echo "standby_mode = on" >> /var/lib/postgresql/data/recovery.conf
    echo "primary_conninfo = host=postgresql port=5432 user=replica" >> /var/lib/postgresql/data/recovery.conf
    
    postgres
    '
```

**Connection Pooling**
```yaml
# PgBouncer para connection pooling
pgbouncer:
  image: pgbouncer/pgbouncer:latest
  environment:
    DATABASES_HOST: postgresql
    DATABASES_PORT: 5432
    DATABASES_USER: postgres
    DATABASES_PASSWORD: ${POSTGRES_PASSWORD}
    DATABASES_DBNAME: ecommerce
    POOL_MODE: transaction
    MAX_CLIENT_CONN: 1000
    DEFAULT_POOL_SIZE: 25
  ports:
    - "6432:6432"
```

### Performance Tuning

**postgresql.conf**
```ini
# Memory
shared_buffers = 256MB
effective_cache_size = 1GB
work_mem = 4MB
maintenance_work_mem = 64MB

# Checkpoints
checkpoint_completion_target = 0.9
wal_buffers = 16MB

# Query Planner
random_page_cost = 1.1
effective_io_concurrency = 200

# Logging
log_statement = 'mod'
log_duration = on
log_min_duration_statement = 1000

# Monitoring
shared_preload_libraries = 'pg_stat_statements'
pg_stat_statements.track = all
```

### Monitoramento

**Métricas Principais**
```sql
-- Queries mais lentas
SELECT query, calls, total_time, mean_time, rows
FROM pg_stat_statements
ORDER BY total_time DESC
LIMIT 10;

-- Conexões ativas
SELECT count(*) as active_connections,
       state,
       application_name
FROM pg_stat_activity
WHERE state IS NOT NULL
GROUP BY state, application_name;

-- Tamanho dos bancos
SELECT datname,
       pg_size_pretty(pg_database_size(datname)) as size
FROM pg_database
ORDER BY pg_database_size(datname) DESC;

-- Índices não utilizados
SELECT schemaname, tablename, indexname, idx_scan
FROM pg_stat_user_indexes
WHERE idx_scan = 0
ORDER BY schemaname, tablename;
```

### Backup Strategy

**Backup Automatizado**
```bash
#!/bin/bash
# backup-script.sh

DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backups"
DATABASES=("order_db" "billing_db" "inventory_db")

for db in "${DATABASES[@]}"; do
    echo "Backing up $db..."
    pg_dump -h localhost -U postgres -d $db | gzip > "$BACKUP_DIR/${db}_$DATE.sql.gz"
    
    # Manter apenas últimos 7 dias
    find $BACKUP_DIR -name "${db}_*.sql.gz" -mtime +7 -delete
done

echo "Backup completed at $(date)"
```

## Padrões e Convenções

### Naming Conventions

```sql
-- Tabelas: snake_case, plural
CREATE TABLE order_items (...)

-- Colunas: snake_case
CREATE TABLE orders (
    customer_id BIGINT,
    created_at TIMESTAMP
);

-- Índices: idx_table_column(s)
CREATE INDEX idx_orders_customer_id ON orders(customer_id);

-- Foreign Keys: fk_table_referenced_table
ALTER TABLE order_items 
ADD CONSTRAINT fk_order_items_orders 
FOREIGN KEY (order_id) REFERENCES orders(id);

-- Constraints: table_column_check
ALTER TABLE orders 
ADD CONSTRAINT orders_total_amount_check 
CHECK (total_amount >= 0);
```

### Data Types Standards

```sql
-- IDs: BIGSERIAL para auto-increment
id BIGSERIAL PRIMARY KEY

-- Timestamps: TIMESTAMP WITH TIME ZONE
created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP

-- Money: DECIMAL(10,2)
total_amount DECIMAL(10,2)

-- Status: VARCHAR com CHECK constraint
status VARCHAR(20) CHECK (status IN ('PENDING', 'CONFIRMED'))

-- JSON data: JSONB (não JSON)
attributes JSONB
```

## Métricas de Sucesso

- **Query Performance**: 95% das queries < 100ms
- **Connection Pool**: Utilização < 80%
- **Database Size**: Crescimento controlado < 20% por mês
- **Backup Success**: 100% de backups bem-sucedidos
- **Uptime**: > 99.9% de disponibilidade
- **Replication Lag**: < 1 segundo para read replicas

## Evolução Futura

### PostgreSQL 16+ Features
- **Logical Replication**: Melhorias de performance
- **Parallel Queries**: Mais operações paralelizáveis
- **JSON**: Novas funções e operadores
- **Monitoring**: Métricas mais detalhadas

### Extensões Planejadas
- **PostGIS**: Para funcionalidades geográficas
- **pg_partman**: Gerenciamento automático de partições
- **pg_stat_kcache**: Métricas de sistema operacional
- **pg_repack**: Reorganização online de tabelas

### Sharding Strategy
```sql
-- Preparação para sharding futuro
-- Partition por customer_id ou date ranges
CREATE TABLE orders_2024_q1 PARTITION OF orders
FOR VALUES FROM ('2024-01-01') TO ('2024-04-01');

CREATE TABLE orders_2024_q2 PARTITION OF orders
FOR VALUES FROM ('2024-04-01') TO ('2024-07-01');
```

## Revisão

Esta decisão deve ser revisada:
- **Mensalmente**: Análise de performance e crescimento
- **Trimestralmente**: Avaliação de custos e escalabilidade
- **Semestralmente**: Comparação com alternativas (cloud databases)
- **Anualmente**: Estratégia de sharding e distribuição

## Referências

- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [PostgreSQL Performance Tuning](https://wiki.postgresql.org/wiki/Performance_Optimization)
- [Spring Boot with PostgreSQL](https://spring.io/guides/gs/accessing-data-jpa/)
- [Database per Service Pattern](https://microservices.io/patterns/data/database-per-service.html)