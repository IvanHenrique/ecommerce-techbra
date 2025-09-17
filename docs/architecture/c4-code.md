# C4 Model - Code Diagram

## Visão Geral

O Code Diagram representa o nível mais detalhado da arquitetura, mostrando as principais classes, interfaces e suas relações dentro dos microserviços.

## Order Service - Code Structure

```mermaid
classDiagram
    class OrderController {
        -OrderService orderService
        +createOrder(CreateOrderRequest) ResponseEntity~OrderResponse~
        +getOrder(Long orderId) ResponseEntity~OrderResponse~
        +getOrdersByCustomer(Long customerId) ResponseEntity~List~OrderResponse~~
        +cancelOrder(Long orderId) ResponseEntity~Void~
    }
    
    class OrderService {
        -OrderRepository orderRepository
        -SagaOrchestrator sagaOrchestrator
        -CacheService cacheService
        -MetricsService metricsService
        +createOrder(CreateOrderCommand) Order
        +getOrder(Long orderId) Order
        +updateOrderStatus(Long orderId, OrderStatus status) Order
        +cancelOrder(Long orderId) void
    }
    
    class SagaOrchestrator {
        -KafkaProducer kafkaProducer
        -SagaStateRepository sagaStateRepository
        +startOrderSaga(Order order) void
        +handlePaymentProcessed(PaymentProcessedEvent event) void
        +handleStockReserved(StockReservedEvent event) void
        +compensateOrder(Long orderId) void
    }
    
    class Order {
        -Long id
        -Long customerId
        -List~OrderItem~ items
        -OrderStatus status
        -BigDecimal totalAmount
        -LocalDateTime createdAt
        +calculateTotal() BigDecimal
        +addItem(OrderItem item) void
        +updateStatus(OrderStatus status) void
        +canBeCancelled() boolean
    }
    
    class OrderItem {
        -Long id
        -Long productId
        -String productName
        -Integer quantity
        -BigDecimal unitPrice
        +getSubtotal() BigDecimal
    }
    
    class OrderRepository {
        <<interface>>
        +save(Order order) Order
        +findById(Long id) Optional~Order~
        +findByCustomerId(Long customerId) List~Order~
        +findByStatus(OrderStatus status) List~Order~
    }
    
    class OrderStatus {
        <<enumeration>>
        PENDING
        PAYMENT_PROCESSING
        STOCK_RESERVED
        CONFIRMED
        CANCELLED
        COMPLETED
    }
    
    OrderController --> OrderService
    OrderService --> OrderRepository
    OrderService --> SagaOrchestrator
    Order --> OrderItem
    Order --> OrderStatus
    SagaOrchestrator --> Order
```

## Billing Service - Code Structure

```mermaid
classDiagram
    class BillingController {
        -PaymentService paymentService
        +processPayment(ProcessPaymentRequest) ResponseEntity~PaymentResponse~
        +getPayment(Long paymentId) ResponseEntity~PaymentResponse~
        +refundPayment(Long paymentId) ResponseEntity~Void~
    }
    
    class PaymentService {
        -BillingRepository billingRepository
        -PaymentGatewayClient paymentGatewayClient
        -KafkaProducer kafkaProducer
        +processPayment(ProcessPaymentCommand) Payment
        +getPayment(Long paymentId) Payment
        +refundPayment(Long paymentId) void
        +handleOrderCreated(OrderCreatedEvent event) void
    }
    
    class PaymentGatewayClient {
        <<interface>>
        +authorizePayment(PaymentRequest) PaymentResponse
        +capturePayment(String transactionId) PaymentResponse
        +refundPayment(String transactionId, BigDecimal amount) PaymentResponse
    }
    
    class Payment {
        -Long id
        -Long orderId
        -String transactionId
        -BigDecimal amount
        -PaymentStatus status
        -PaymentMethod method
        -LocalDateTime processedAt
        +isSuccessful() boolean
        +canBeRefunded() boolean
        +updateStatus(PaymentStatus status) void
    }
    
    class BillingRepository {
        <<interface>>
        +save(Payment payment) Payment
        +findById(Long id) Optional~Payment~
        +findByOrderId(Long orderId) Optional~Payment~
        +findByTransactionId(String transactionId) Optional~Payment~
    }
    
    class PaymentStatus {
        <<enumeration>>
        PENDING
        AUTHORIZED
        CAPTURED
        FAILED
        REFUNDED
        CANCELLED
    }
    
    class PaymentMethod {
        <<enumeration>>
        CREDIT_CARD
        DEBIT_CARD
        PIX
        BANK_TRANSFER
    }
    
    BillingController --> PaymentService
    PaymentService --> BillingRepository
    PaymentService --> PaymentGatewayClient
    Payment --> PaymentStatus
    Payment --> PaymentMethod
```

## Inventory Service - Code Structure

```mermaid
classDiagram
    class InventoryController {
        -InventoryService inventoryService
        +getProduct(Long productId) ResponseEntity~ProductResponse~
        +updateStock(Long productId, UpdateStockRequest) ResponseEntity~Void~
        +checkAvailability(Long productId, Integer quantity) ResponseEntity~Boolean~
        +reserveStock(ReserveStockRequest) ResponseEntity~Void~
    }
    
    class InventoryService {
        -InventoryRepository inventoryRepository
        -StockManager stockManager
        -CacheService cacheService
        +getProduct(Long productId) Product
        +updateStock(Long productId, Integer quantity) void
        +checkAvailability(Long productId, Integer quantity) boolean
        +reserveStock(Long productId, Integer quantity, Long orderId) void
        +releaseStock(Long productId, Integer quantity, Long orderId) void
    }
    
    class StockManager {
        -LockService lockService
        -KafkaProducer kafkaProducer
        +reserveStock(Product product, Integer quantity, Long orderId) StockReservation
        +releaseReservation(StockReservation reservation) void
        +confirmReservation(StockReservation reservation) void
    }
    
    class Product {
        -Long id
        -String name
        -String description
        -BigDecimal price
        -Integer availableStock
        -Integer reservedStock
        -ProductStatus status
        +getTotalStock() Integer
        +canReserve(Integer quantity) boolean
        +reserveStock(Integer quantity) void
        +releaseStock(Integer quantity) void
    }
    
    class StockReservation {
        -Long id
        -Long productId
        -Long orderId
        -Integer quantity
        -ReservationStatus status
        -LocalDateTime createdAt
        -LocalDateTime expiresAt
        +isExpired() boolean
        +confirm() void
        +release() void
    }
    
    class InventoryRepository {
        <<interface>>
        +save(Product product) Product
        +findById(Long id) Optional~Product~
        +findByStatus(ProductStatus status) List~Product~
        +findLowStockProducts(Integer threshold) List~Product~
    }
    
    class ProductStatus {
        <<enumeration>>
        ACTIVE
        INACTIVE
        OUT_OF_STOCK
        DISCONTINUED
    }
    
    class ReservationStatus {
        <<enumeration>>
        PENDING
        CONFIRMED
        RELEASED
        EXPIRED
    }
    
    InventoryController --> InventoryService
    InventoryService --> InventoryRepository
    InventoryService --> StockManager
    Product --> ProductStatus
    StockReservation --> ReservationStatus
    StockManager --> Product
    StockManager --> StockReservation
```

## BFF - Code Structure

```mermaid
classDiagram
    class OrderController {
        -AggregationService aggregationService
        -AuthService authService
        +createOrder(CreateOrderRequest) ResponseEntity~OrderResponse~
        +getOrderDetails(Long orderId) ResponseEntity~OrderDetailsResponse~
        +getCustomerOrders(Long customerId) ResponseEntity~List~OrderSummaryResponse~~
    }
    
    class ProductController {
        -AggregationService aggregationService
        +getProducts(ProductSearchRequest) ResponseEntity~List~ProductResponse~~
        +getProductDetails(Long productId) ResponseEntity~ProductDetailsResponse~
    }
    
    class AggregationService {
        -OrderClient orderClient
        -BillingClient billingClient
        -InventoryClient inventoryClient
        +getOrderDetails(Long orderId) OrderDetailsResponse
        +getProductDetails(Long productId) ProductDetailsResponse
        +createOrder(CreateOrderRequest) OrderResponse
    }
    
    class OrderClient {
        <<interface>>
        +createOrder(CreateOrderRequest) OrderResponse
        +getOrder(Long orderId) OrderResponse
        +getOrdersByCustomer(Long customerId) List~OrderResponse~
    }
    
    class BillingClient {
        <<interface>>
        +getPayment(Long orderId) PaymentResponse
        +processPayment(ProcessPaymentRequest) PaymentResponse
    }
    
    class InventoryClient {
        <<interface>>
        +getProduct(Long productId) ProductResponse
        +checkAvailability(Long productId, Integer quantity) Boolean
    }
    
    class OrderDetailsResponse {
        -OrderResponse order
        -PaymentResponse payment
        -List~ProductResponse~ products
        +getTotalAmount() BigDecimal
        +getOrderStatus() String
    }
    
    class AuthService {
        +validateToken(String token) boolean
        +extractUserId(String token) Long
        +hasPermission(Long userId, String resource) boolean
    }
    
    OrderController --> AggregationService
    ProductController --> AggregationService
    OrderController --> AuthService
    AggregationService --> OrderClient
    AggregationService --> BillingClient
    AggregationService --> InventoryClient
    AggregationService --> OrderDetailsResponse
```

## Shared Kernel - Common Classes

```mermaid
classDiagram
    class BaseEntity {
        <<abstract>>
        -Long id
        -LocalDateTime createdAt
        -LocalDateTime updatedAt
        -String createdBy
        -String updatedBy
        +getId() Long
        +getCreatedAt() LocalDateTime
    }
    
    class DomainEvent {
        <<abstract>>
        -String eventId
        -String eventType
        -LocalDateTime occurredAt
        -String aggregateId
        +getEventId() String
        +getEventType() String
    }
    
    class OrderCreatedEvent {
        -Long orderId
        -Long customerId
        -List~OrderItemDto~ items
        -BigDecimal totalAmount
    }
    
    class PaymentProcessedEvent {
        -Long orderId
        -String transactionId
        -BigDecimal amount
        -PaymentStatus status
    }
    
    class StockReservedEvent {
        -Long orderId
        -Long productId
        -Integer quantity
        -String reservationId
    }
    
    class ApiResponse {
        <<generic>>
        -T data
        -String message
        -boolean success
        -List~String~ errors
        +success(T data) ApiResponse~T~
        +error(String message) ApiResponse~T~
    }
    
    class PagedResponse {
        <<generic>>
        -List~T~ content
        -int page
        -int size
        -long totalElements
        -int totalPages
    }
    
    DomainEvent <|-- OrderCreatedEvent
    DomainEvent <|-- PaymentProcessedEvent
    DomainEvent <|-- StockReservedEvent
```

## Design Patterns Utilizados

### Creational Patterns
- **Factory Pattern**: Criação de eventos de domínio
- **Builder Pattern**: Construção de objetos complexos (Order, Payment)

### Structural Patterns
- **Adapter Pattern**: Clientes Feign para integração
- **Facade Pattern**: BFF como fachada para microserviços
- **Decorator Pattern**: Aspectos de segurança e cache

### Behavioral Patterns
- **Strategy Pattern**: Diferentes métodos de pagamento
- **Observer Pattern**: Event listeners do Kafka
- **Command Pattern**: Commands para operações de negócio
- **State Pattern**: Estados de pedidos e pagamentos

## Convenções de Código

### Nomenclatura
- **Classes**: PascalCase (OrderService, PaymentController)
- **Métodos**: camelCase (createOrder, processPayment)
- **Constantes**: UPPER_SNAKE_CASE (ORDER_CREATED_TOPIC)
- **Packages**: lowercase (com.techbra.order.domain)

### Estrutura de Packages
```
com.techbra.{service}
├── controller/     # REST endpoints
├── service/        # Business logic
├── repository/     # Data access
├── domain/         # Domain entities
├── dto/           # Data transfer objects
├── config/        # Configuration classes
├── exception/     # Custom exceptions
└── event/         # Domain events
```

### Anotações Principais
- `@RestController`: Controllers REST
- `@Service`: Services de negócio
- `@Repository`: Repositórios de dados
- `@Entity`: Entidades JPA
- `@EventListener`: Listeners de eventos
- `@Transactional`: Controle transacional
- `@Cacheable`: Cache de métodos
- `@CircuitBreaker`: Proteção contra falhas

## Validações e Constraints

### Bean Validation
- `@NotNull`: Campos obrigatórios
- `@NotBlank`: Strings não vazias
- `@Min/@Max`: Valores numéricos
- `@Email`: Formato de email
- `@Valid`: Validação cascata

### Custom Validators
- `@ValidOrderStatus`: Status válido de pedido
- `@ValidPaymentMethod`: Método de pagamento válido
- `@ValidStock`: Quantidade de estoque válida

Esta estrutura de código garante alta coesão, baixo acoplamento e facilita a manutenção e evolução do sistema.