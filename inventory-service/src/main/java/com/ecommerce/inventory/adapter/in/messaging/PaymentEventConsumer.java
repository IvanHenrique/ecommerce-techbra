package com.ecommerce.inventory.adapter.in.messaging;

import com.ecommerce.inventory.application.port.in.ReserveInventoryCommand;
import com.ecommerce.inventory.application.port.in.ReserveInventoryItemCommand;
import com.ecommerce.inventory.application.port.in.ReserveInventoryUseCase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class PaymentEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final ReserveInventoryUseCase reserveInventoryUseCase;
    private final ObjectMapper objectMapper;
    private final Counter paymentEventsProcessedCounter;
    private final Counter paymentEventsFailedCounter;

    public PaymentEventConsumer(ReserveInventoryUseCase reserveInventoryUseCase,
                               ObjectMapper objectMapper,
                               MeterRegistry meterRegistry) {
        this.reserveInventoryUseCase = reserveInventoryUseCase;
        this.objectMapper = objectMapper;
        this.paymentEventsProcessedCounter = Counter.builder("inventory.payment.events.processed")
                .description("Number of payment events processed")
                .register(meterRegistry);
        this.paymentEventsFailedCounter = Counter.builder("inventory.payment.events.failed")
                .description("Number of payment events failed to process")
                .register(meterRegistry);
    }

    @KafkaListener(
        topics = "billing.events",
        groupId = "inventory-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentEvent(
            @Payload String eventPayload,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        logger.info("Received payment event from topic: {}, partition: {}, offset: {}, key: {}", 
            topic, partition, offset, key);

        try {
            var paymentEvent = objectMapper.readValue(eventPayload, PaymentCompletedEventDto.class);
            
            // Only process PaymentCompleted events
            if ("PaymentCompleted".equals(paymentEvent.eventType())) {
                processPaymentCompletedEvent(paymentEvent);
                paymentEventsProcessedCounter.increment();
                logger.info("Successfully processed PaymentCompleted event for order: {}", paymentEvent.orderId());
            } else {
                logger.debug("Ignoring event type: {} for order: {}", 
                    paymentEvent.eventType(), paymentEvent.orderId());
            }

            // Manual acknowledgment after successful processing
            acknowledgment.acknowledge();

        } catch (JsonProcessingException ex) {
            logger.error("Failed to deserialize payment event payload: {}", eventPayload, ex);
            paymentEventsFailedCounter.increment();
            acknowledgment.acknowledge();
            
        } catch (Exception ex) {
            logger.error("Failed to process payment event. Payload: {}", eventPayload, ex);
            paymentEventsFailedCounter.increment();
            acknowledgment.acknowledge();
        }
    }

    private void processPaymentCompletedEvent(PaymentCompletedEventDto paymentEvent) {
        // Generate idempotency key based on order ID
        String idempotencyKey = "payment-" + paymentEvent.orderId().toString();
        
        // For MVP, we'll create mock inventory items based on the payment amount
        // In a real system, this information would come from the order details
        var mockItems = createMockInventoryItems(paymentEvent);
        
        var command = new ReserveInventoryCommand(
            paymentEvent.orderId(),
            paymentEvent.customerId(),
            mockItems,
            idempotencyKey
        );

        var result = reserveInventoryUseCase.execute(command);
        
        if (result.isFailure()) {
            logger.error("Failed to reserve inventory for order: {}. Error: {}", 
                paymentEvent.orderId(), result.getErrorMessage());
            throw new RuntimeException("Inventory reservation failed: " + result.getErrorMessage());
        }
        
        logger.info("Inventory reserved successfully for order: {}. Reference: {}", 
            paymentEvent.orderId(), result.getValue().reservationReference());
    }

    private List<ReserveInventoryItemCommand> createMockInventoryItems(PaymentCompletedEventDto paymentEvent) {
        // For MVP, create mock products based on payment amount
        // In real system, would fetch order details to get actual products
        
        if (paymentEvent.amount().doubleValue() > 1000.0) {
            // High-value orders - multiple items
            return List.of(
                new ReserveInventoryItemCommand(
                    UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), 
                    "Premium Product", 
                    1
                ),
                new ReserveInventoryItemCommand(
                    UUID.fromString("987fcdeb-51a2-43d1-9c4e-5f6a789012bc"), 
                    "Accessory", 
                    2
                )
            );
        } else {
            // Regular orders - single item
            return List.of(
                new ReserveInventoryItemCommand(
                    UUID.fromString("456e7890-e89b-12d3-a456-426614174001"), 
                    "Standard Product", 
                    1
                )
            );
        }
    }
}