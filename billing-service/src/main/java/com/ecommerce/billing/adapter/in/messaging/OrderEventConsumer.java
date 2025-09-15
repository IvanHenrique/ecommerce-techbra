package com.ecommerce.billing.adapter.in.messaging;

import com.ecommerce.billing.application.port.in.ProcessPaymentCommand;
import com.ecommerce.billing.application.port.in.ProcessPaymentUseCase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Observed(name = "kafka.consumer", contextualName = "order-events-consumer")
public class OrderEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final ProcessPaymentUseCase processPaymentUseCase;
    private final ObjectMapper objectMapper;
    private final Counter orderEventsProcessedCounter;
    private final Counter orderEventsFailedCounter;

    public OrderEventConsumer(ProcessPaymentUseCase processPaymentUseCase,
                             ObjectMapper objectMapper,
                             MeterRegistry meterRegistry) {
        this.processPaymentUseCase = processPaymentUseCase;
        this.objectMapper = objectMapper;
        this.orderEventsProcessedCounter = Counter.builder("billing.order.events.processed")
                .description("Number of order events processed")
                .register(meterRegistry);
        this.orderEventsFailedCounter = Counter.builder("billing.order.events.failed")
                .description("Number of order events failed to process")
                .register(meterRegistry);
    }

    @KafkaListener(
        topics = "order.events",
        groupId = "billing-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderEvent(
            @Payload String eventPayload,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        logger.info("Received order event from topic: {}, partition: {}, offset: {}, key: {}", 
            topic, partition, offset, key);

        try {
            var orderEvent = objectMapper.readValue(eventPayload, OrderCreatedEventDto.class);
            
            // Only process OrderCreated events
            if ("OrderCreated".equals(orderEvent.eventType())) {
                processOrderCreatedEvent(orderEvent);
                orderEventsProcessedCounter.increment();
                logger.info("Successfully processed OrderCreated event for order: {}", orderEvent.aggregateId());
            } else {
                logger.debug("Ignoring event type: {} for order: {}", 
                    orderEvent.eventType(), orderEvent.aggregateId());
            }

            // Manual acknowledgment after successful processing
            acknowledgment.acknowledge();

        } catch (JsonProcessingException ex) {
            logger.error("Failed to deserialize order event payload: {}", eventPayload, ex);
            orderEventsFailedCounter.increment();
            // Still acknowledge to avoid infinite retries for malformed messages
            acknowledgment.acknowledge();
            
        } catch (Exception ex) {
            logger.error("Failed to process order event. Payload: {}", eventPayload, ex);
            orderEventsFailedCounter.increment();
            // Could implement retry logic here or send to DLQ
            acknowledgment.acknowledge();
        }
    }

    private void processOrderCreatedEvent(OrderCreatedEventDto orderEvent) {
        // Generate idempotency key based on order ID
        String idempotencyKey = "order-" + orderEvent.aggregateId().toString();
        
        // Default payment method for MVP
        String defaultPaymentMethod = "CREDIT_CARD";
        
        var command = new ProcessPaymentCommand(
            orderEvent.aggregateId(),  // orderId
            orderEvent.customerId(),
            orderEvent.totalAmount(),
            orderEvent.currency(),
            defaultPaymentMethod,
            idempotencyKey
        );

        var result = processPaymentUseCase.execute(command);
        
        if (result.isFailure()) {
            logger.error("Failed to process payment for order: {}. Error: {}", 
                orderEvent.aggregateId(), result.getErrorMessage());
            throw new RuntimeException("Payment processing failed: " + result.getErrorMessage());
        }
        
        logger.info("Payment processed successfully for order: {}. Payment ID: {}", 
            orderEvent.aggregateId(), result.getValue().paymentId());
    }
}