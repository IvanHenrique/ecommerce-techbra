# ADR-005: Redis como Cache Distribuído

## Status
**Aceita** - 2024-01-15

## Contexto

O sistema e-commerce Techbra precisa de uma solução de cache que atenda aos seguintes requisitos:

- **Performance**: Redução de latência para dados frequentemente acessados
- **Escalabilidade**: Suporte a múltiplas instâncias de microserviços
- **Consistência**: Cache distribuído entre instâncias
- **Flexibilidade**: Suporte a diferentes estruturas de dados
- **Observabilidade**: Métricas e monitoramento de cache
- **Persistência**: Opção de persistir dados críticos
- **Session Management**: Gerenciamento de sessões de usuário
- **Rate Limiting**: Controle de taxa de requisições

As opções consideradas foram:

1. **Redis** (In-memory data structure store)
2. **Memcached** (Distributed memory caching)
3. **Hazelcast** (In-memory data grid)
4. **Apache Ignite** (In-memory computing platform)
5. **Caffeine** (Local cache - Java)
6. **EhCache** (Local cache - Java)

## Decisão

Adotamos **Redis 7.x** como solução de cache distribuído para o sistema e-commerce Techbra.

### Configuração Base

```yaml
# docker-compose.yml
redis:
  image: redis:7-alpine
  command: redis-server --appendonly yes --maxmemory 512mb --maxmemory-policy allkeys-lru
  ports:
    - "6379:6379"
  volumes:
    - redis_data:/data
    - ./infrastructure/redis/redis.conf:/usr/local/etc/redis/redis.conf
  environment:
    - REDIS_PASSWORD=${REDIS_PASSWORD}
  healthcheck:
    test: ["CMD", "redis-cli", "ping"]
    interval: 30s
    timeout: 10s
    retries: 3
```

## Justificativa da Decisão

### Comparação Detalhada

| Critério | Redis | Memcached | Hazelcast | Ignite | Caffeine | EhCache |
|----------|-------|-----------|-----------|--------|----------|----------|
| **Performance** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **Estruturas de Dados** | ⭐⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐ |
| **Persistência** | ⭐⭐⭐⭐⭐ | ⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐ | ⭐⭐⭐ |
| **Clustering** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐ | ⭐⭐⭐ |
| **Maturidade** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ |
| **Comunidade** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ |
| **Operação** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **Memory Efficiency** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **Distribuído** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐ | ⭐⭐ |

### Vantagens do Redis

✅ **Estruturas de Dados Ricas**
- Strings, Hashes, Lists, Sets, Sorted Sets
- Bitmaps, HyperLogLogs, Streams
- Geospatial indexes
- JSON support (RedisJSON)

✅ **Performance Excepcional**
- Operações em memória com latência sub-milissegundo
- Pipeline para batch operations
- Lua scripting para operações atômicas
- Multiplexing de conexões

✅ **Persistência Flexível**
- RDB snapshots para backup
- AOF (Append Only File) para durabilidade
- Hybrid persistence (RDB + AOF)

✅ **Clustering Nativo**
- Redis Cluster para sharding automático
- Master-Slave replication
- Sentinel para high availability
- Automatic failover

✅ **Funcionalidades Avançadas**
- Pub/Sub messaging
- Transactions com MULTI/EXEC
- Expiration policies
- Memory optimization

✅ **Observabilidade**
- INFO command para métricas
- MONITOR para debugging
- Slow log para queries lentas
- Integração com Prometheus

## Implementação

### Casos de Uso no E-commerce

#### 1. Cache de Produtos
```java
@Service
public class ProductCacheService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ProductRepository productRepository;
    
    private static final String PRODUCT_CACHE_KEY = "product:";
    private static final Duration CACHE_TTL = Duration.ofHours(1);
    
    public Product getProduct(Long productId) {
        String cacheKey = PRODUCT_CACHE_KEY + productId;
        
        // Tentar buscar no cache primeiro
        Product cachedProduct = (Product) redisTemplate.opsForValue().get(cacheKey);
        if (cachedProduct != null) {
            return cachedProduct;
        }
        
        // Se não encontrar, buscar no banco
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));
        
        // Armazenar no cache
        redisTemplate.opsForValue().set(cacheKey, product, CACHE_TTL);
        return product;
    }
    
    public void invalidateProduct(Long productId) {
        String cacheKey = PRODUCT_CACHE_KEY + productId;
        redisTemplate.delete(cacheKey);
    }
}
```

#### 2. Session Management
```java
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 3600)
public class SessionConfig {
    
    @Bean
    public LettuceConnectionFactory connectionFactory() {
        return new LettuceConnectionFactory(
            new RedisStandaloneConfiguration("localhost", 6379)
        );
    }
}

// Uso em controller
@RestController
public class AuthController {
    
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        
        // Autenticar usuário
        User user = authService.authenticate(request.getEmail(), request.getPassword());
        
        // Criar sessão (automaticamente armazenada no Redis)
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute("userId", user.getId());
        session.setAttribute("userRole", user.getRole());
        
        return ResponseEntity.ok(new LoginResponse(user, session.getId()));
    }
}
```

#### 3. Rate Limiting
```java
@Component
public class RateLimitService {
    private final RedisTemplate<String, String> redisTemplate;
    
    public boolean isAllowed(String clientId, int maxRequests, Duration window) {
        String key = "rate_limit:" + clientId;
        String currentTime = String.valueOf(System.currentTimeMillis());
        
        // Usar Lua script para operação atômica
        String luaScript = """
            local key = KEYS[1]
            local window = tonumber(ARGV[1])
            local limit = tonumber(ARGV[2])
            local current_time = tonumber(ARGV[3])
            
            -- Remove entradas antigas
            redis.call('ZREMRANGEBYSCORE', key, 0, current_time - window)
            
            -- Conta requisições atuais
            local current_requests = redis.call('ZCARD', key)
            
            if current_requests < limit then
                -- Adiciona nova requisição
                redis.call('ZADD', key, current_time, current_time)
                redis.call('EXPIRE', key, math.ceil(window / 1000))
                return 1
            else
                return 0
            end
        """;
        
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(luaScript);
        script.setResultType(Long.class);
        
        Long result = redisTemplate.execute(script, 
            Collections.singletonList(key),
            String.valueOf(window.toMillis()),
            String.valueOf(maxRequests),
            currentTime
        );
        
        return result != null && result == 1L;
    }
}
```

#### 4. Cache de Carrinho de Compras
```java
@Service
public class ShoppingCartService {
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String CART_KEY_PREFIX = "cart:";
    private static final Duration CART_TTL = Duration.ofDays(7);
    
    public void addToCart(String sessionId, Long productId, Integer quantity) {
        String cartKey = CART_KEY_PREFIX + sessionId;
        
        // Usar Hash para armazenar itens do carrinho
        redisTemplate.opsForHash().put(cartKey, 
            productId.toString(), 
            quantity.toString()
        );
        
        // Renovar TTL
        redisTemplate.expire(cartKey, CART_TTL);
    }
    
    public Map<String, String> getCart(String sessionId) {
        String cartKey = CART_KEY_PREFIX + sessionId;
        return redisTemplate.opsForHash().entries(cartKey);
    }
    
    public void removeFromCart(String sessionId, Long productId) {
        String cartKey = CART_KEY_PREFIX + sessionId;
        redisTemplate.opsForHash().delete(cartKey, productId.toString());
    }
    
    public void clearCart(String sessionId) {
        String cartKey = CART_KEY_PREFIX + sessionId;
        redisTemplate.delete(cartKey);
    }
}
```

#### 5. Cache de Consultas Complexas
```java
@Service
public class OrderAnalyticsService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final OrderRepository orderRepository;
    
    @Cacheable(value = "order-stats", key = "#customerId + ':' + #period")
    public OrderStatistics getCustomerOrderStats(Long customerId, String period) {
        // Esta consulta será cacheada automaticamente
        return orderRepository.calculateOrderStatistics(customerId, period);
    }
    
    @CacheEvict(value = "order-stats", key = "#order.customerId + ':*'")
    public void invalidateCustomerStats(Order order) {
        // Cache será invalidado quando novo pedido for criado
    }
}
```

### Configuração do Spring Boot

**application.yml**
```yaml
spring:
  # Redis Configuration
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
    timeout: 2000ms
    
    # Connection Pool (Lettuce)
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
        max-wait: 2000ms
    
    # Cluster configuration (for production)
    cluster:
      nodes: ${REDIS_CLUSTER_NODES:localhost:7000,localhost:7001,localhost:7002}
      max-redirects: 3
  
  # Cache Configuration
  cache:
    type: redis
    redis:
      time-to-live: 3600000 # 1 hour
      cache-null-values: false
      key-prefix: "techbra:cache:"
      use-key-prefix: true

# Session Configuration
server:
  servlet:
    session:
      timeout: 30m
      cookie:
        max-age: 1800
        http-only: true
        secure: ${SESSION_COOKIE_SECURE:false}
```

**RedisConfig.java**
```java
@Configuration
@EnableCaching
public class RedisConfig {
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Serializers
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        template.afterPropertiesSet();
        return template;
    }
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues();
        
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .build();
    }
    
    @Bean
    public RedisScript<Long> rateLimitScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/rate-limit.lua"));
        script.setResultType(Long.class);
        return script;
    }
}
```

### Configuração de Produção

**redis.conf**
```ini
# Network
bind 0.0.0.0
port 6379
protected-mode yes
requirepass ${REDIS_PASSWORD}

# Memory
maxmemory 1gb
maxmemory-policy allkeys-lru

# Persistence
save 900 1
save 300 10
save 60 10000

appendonly yes
appendfsync everysec

# Logging
loglevel notice
logfile "/var/log/redis/redis-server.log"

# Performance
tcp-keepalive 300
timeout 0
tcp-backlog 511

# Security
rename-command FLUSHDB ""
rename-command FLUSHALL ""
rename-command DEBUG ""
```

## Consequências

### Positivas

✅ **Performance Excepcional**
- Latência sub-milissegundo para operações em memória
- Throughput alto com milhares de operações por segundo
- Pipeline para operações em lote

✅ **Flexibilidade de Dados**
- Múltiplas estruturas de dados nativas
- Suporte a operações complexas (intersections, unions)
- Lua scripting para lógica customizada

✅ **Escalabilidade**
- Redis Cluster para sharding automático
- Replicação master-slave
- Partitioning horizontal

✅ **Durabilidade Configurável**
- RDB snapshots para backup
- AOF para durabilidade completa
- Hybrid mode para melhor performance

✅ **Funcionalidades Avançadas**
- Pub/Sub para messaging
- Transactions atômicas
- Expiration automática
- Geospatial operations

✅ **Observabilidade**
- Métricas detalhadas via INFO
- Slow log para debugging
- Memory analysis tools
- Integração com monitoring

### Negativas

❌ **Consumo de Memória**
- Todos os dados ficam em RAM
- Overhead de estruturas de dados
- Necessidade de planejamento de capacidade

❌ **Complexidade Operacional**
- Configuração de cluster
- Monitoramento de memória
- Backup e recovery procedures

❌ **Single-threaded**
- Operações bloqueantes podem impactar performance
- Lua scripts longos bloqueiam outras operações

❌ **Persistência vs Performance**
- Trade-off entre durabilidade e performance
- AOF pode impactar write performance
- Recovery time proporcional ao tamanho dos dados

## Mitigações

### Gestão de Memória

**Monitoring e Alertas**
```yaml
# Prometheus alerts
groups:
  - name: redis
    rules:
      - alert: RedisMemoryHigh
        expr: redis_memory_used_bytes / redis_memory_max_bytes > 0.8
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Redis memory usage is high"
          
      - alert: RedisDown
        expr: up{job="redis"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Redis instance is down"
```

**Memory Optimization**
```java
@Component
public class RedisMemoryOptimizer {
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void optimizeMemory() {
        // Executar MEMORY PURGE para liberar memória fragmentada
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.execute("MEMORY", "PURGE".getBytes());
            return null;
        });
    }
    
    @EventListener
    public void handleMemoryWarning(RedisMemoryWarningEvent event) {
        // Implementar lógica de limpeza de cache menos importante
        cleanupLowPriorityCache();
    }
}
```

### High Availability

**Redis Sentinel**
```yaml
# docker-compose.yml
redis-sentinel-1:
  image: redis:7-alpine
  command: redis-sentinel /etc/redis/sentinel.conf
  volumes:
    - ./infrastructure/redis/sentinel1.conf:/etc/redis/sentinel.conf
  ports:
    - "26379:26379"

redis-sentinel-2:
  image: redis:7-alpine
  command: redis-sentinel /etc/redis/sentinel.conf
  volumes:
    - ./infrastructure/redis/sentinel2.conf:/etc/redis/sentinel.conf
  ports:
    - "26380:26379"

redis-sentinel-3:
  image: redis:7-alpine
  command: redis-sentinel /etc/redis/sentinel.conf
  volumes:
    - ./infrastructure/redis/sentinel3.conf:/etc/redis/sentinel.conf
  ports:
    - "26381:26379"
```

**sentinel.conf**
```ini
port 26379
sentinel monitor mymaster redis-master 6379 2
sentinel auth-pass mymaster ${REDIS_PASSWORD}
sentinel down-after-milliseconds mymaster 5000
sentinel parallel-syncs mymaster 1
sentinel failover-timeout mymaster 10000
```

### Backup Strategy

**Automated Backup**
```bash
#!/bin/bash
# redis-backup.sh

DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backups/redis"
REDIS_HOST="localhost"
REDIS_PORT="6379"

# Create RDB snapshot
redis-cli -h $REDIS_HOST -p $REDIS_PORT BGSAVE

# Wait for backup to complete
while [ $(redis-cli -h $REDIS_HOST -p $REDIS_PORT LASTSAVE) -eq $(redis-cli -h $REDIS_HOST -p $REDIS_PORT LASTSAVE) ]; do
    sleep 1
done

# Copy RDB file
cp /var/lib/redis/dump.rdb "$BACKUP_DIR/dump_$DATE.rdb"

# Compress backup
gzip "$BACKUP_DIR/dump_$DATE.rdb"

# Keep only last 7 days
find $BACKUP_DIR -name "dump_*.rdb.gz" -mtime +7 -delete

echo "Redis backup completed: dump_$DATE.rdb.gz"
```

## Padrões e Convenções

### Naming Conventions

```java
// Padrões de chaves
public class CacheKeys {
    // Formato: service:entity:id
    public static final String PRODUCT = "ecommerce:product:";
    public static final String ORDER = "ecommerce:order:";
    public static final String USER_SESSION = "ecommerce:session:";
    
    // Formato: service:operation:params
    public static final String RATE_LIMIT = "ecommerce:rate_limit:";
    public static final String CART = "ecommerce:cart:";
    
    // TTL constants
    public static final Duration PRODUCT_TTL = Duration.ofHours(2);
    public static final Duration SESSION_TTL = Duration.ofMinutes(30);
    public static final Duration CART_TTL = Duration.ofDays(7);
}
```

### Error Handling

```java
@Component
public class ResilientCacheService {
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Retryable(value = {RedisConnectionFailureException.class}, maxAttempts = 3)
    public <T> T getFromCache(String key, Class<T> type) {
        try {
            return (T) redisTemplate.opsForValue().get(key);
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis connection failed for key: {}", key, e);
            throw e; // Retry will handle this
        } catch (Exception e) {
            log.error("Unexpected error accessing cache for key: {}", key, e);
            return null; // Graceful degradation
        }
    }
    
    @Recover
    public <T> T recoverFromCacheFailure(RedisConnectionFailureException ex, String key, Class<T> type) {
        log.error("Cache completely unavailable for key: {}, falling back to database", key);
        // Implementar fallback para banco de dados
        return null;
    }
}
```

## Métricas de Sucesso

- **Cache Hit Rate**: > 80% para dados de produtos
- **Response Time**: < 5ms para operações de cache
- **Memory Usage**: < 80% da capacidade configurada
- **Availability**: > 99.9% uptime
- **Throughput**: > 10,000 ops/sec por instância
- **Session Performance**: < 10ms para operações de sessão

## Evolução Futura

### Redis 7+ Features
- **Functions**: Substituição do Lua scripting
- **JSON**: Suporte nativo a documentos JSON
- **Search**: Full-text search integrado
- **Time Series**: Dados de séries temporais
- **Probabilistic**: Bloom filters, Count-Min Sketch

### Extensões Planejadas
- **RedisGraph**: Para dados relacionais complexos
- **RedisTimeSeries**: Métricas e analytics
- **RedisAI**: Machine learning inference
- **RedisGears**: Stream processing

### Otimizações
```java
// Pipeline para operações em lote
public void bulkCacheUpdate(Map<String, Object> data) {
    redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
        data.forEach((key, value) -> {
            connection.set(key.getBytes(), serialize(value));
        });
        return null;
    });
}

// Lua script para operações atômicas complexas
public boolean atomicCartUpdate(String cartId, Long productId, Integer quantity) {
    String script = """
        local cart_key = KEYS[1]
        local product_id = ARGV[1]
        local quantity = tonumber(ARGV[2])
        
        if quantity > 0 then
            redis.call('HSET', cart_key, product_id, quantity)
        else
            redis.call('HDEL', cart_key, product_id)
        end
        
        redis.call('EXPIRE', cart_key, 604800) -- 7 days
        return redis.call('HLEN', cart_key)
    """;
    
    Long result = redisTemplate.execute(
        new DefaultRedisScript<>(script, Long.class),
        Collections.singletonList("cart:" + cartId),
        productId.toString(),
        quantity.toString()
    );
    
    return result != null && result >= 0;
}
```

## Revisão

Esta decisão deve ser revisada:
- **Mensalmente**: Análise de performance e uso de memória
- **Trimestralmente**: Avaliação de custos e escalabilidade
- **Semestralmente**: Comparação com alternativas (KeyDB, Dragonfly)
- **Anualmente**: Estratégia de clustering e sharding

## Referências

- [Redis Documentation](https://redis.io/documentation)
- [Spring Data Redis](https://spring.io/projects/spring-data-redis)
- [Redis Best Practices](https://redis.io/topics/memory-optimization)
- [Redis Cluster Tutorial](https://redis.io/topics/cluster-tutorial)
- [Redis Sentinel](https://redis.io/topics/sentinel)

---

**Nota sobre Arquitetura vs Implementação Atual:**

É completamente normal e até recomendado que os diagramas C4 mostrem a arquitetura completa planejada, mesmo que nem todas as funcionalidades estejam implementadas ainda. Os diagramas servem como:

1. **Guia de Desenvolvimento**: Mostram o destino final da arquitetura
2. **Comunicação**: Facilitam discussões com stakeholders
3. **Planejamento**: Ajudam a priorizar próximas implementações
4. **Consistência**: Garantem que o desenvolvimento siga uma visão unificada

O fato de você ter apenas o fluxo de criação de pedido implementado não é problema - é um desenvolvimento incremental saudável. Os diagramas C4 representam a **arquitetura alvo**, não necessariamente o estado atual da implementação.