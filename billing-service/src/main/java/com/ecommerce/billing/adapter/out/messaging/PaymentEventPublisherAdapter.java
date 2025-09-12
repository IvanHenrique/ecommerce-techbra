package com.ecommerce.billing.adapter.out.messaging;

import com.ecommerce.billing.application.port.out.PaymentEventPublisherPort;
import com.ecommerce.billing.domain.event.PaymentCompletedEvent;
import com.ecommerce.billing.domain.event.PaymentFailedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class PaymentEventPublisherAdapter implements PaymentEventPublisherPort {

    private static final Logger logger = LoggerFactory.getLogger(PaymentEventPublisherAdapter.class);
    private static final String BILLING_EVENTS_TOPIC = "billing.events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public PaymentEventPublisherAdapter(KafkaTemplate<String, String> kafkaTemplate,
                                       ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        publishEvent(event, "PaymentCompleted");
    }

    @Override
    public void publishPaymentFailed(PaymentFailedEvent event) {
        publishEvent(event, "PaymentFailed");
    }

    private void publishEvent(Object event, String eventType) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            String key = extractKeyFromEvent(event);

            CompletableFuture<SendResult<String, String>> future = 
                kafkaTemplate.send(BILLING_EVENTS_TOPIC, key, eventJson);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("{} event published successfully. Topic: {}, Key: {}, Offset: {}", 
                        eventType, BILLING_EVENTS_TOPIC, key, result.getRecordMetadata().offset());
                } else {
                    logger.error("Failed to publish {} event. Topic: {}, Key: {}", 
                        eventType, BILLING_EVENTS_TOPIC, key, ex);
                }
            });

        } catch (JsonProcessingException ex) {
            logger.error("Failed to serialize {} event", eventType, ex);
            throw new RuntimeException("Failed to publish event", ex);
        }
    }

    private String extractKeyFromEvent(Object event) {
        if (event instanceof PaymentCompletedEvent completedEvent) {
            return completedEvent.orderId().toString();
        } else if (event instanceof PaymentFailedEvent failedEvent) {
            return failedEvent.orderId().toString();
        }
        return "unknown";
    }
}