package com.ecommerce.inventory.adapter.out.messaging;

import com.ecommerce.inventory.application.port.out.InventoryEventPublisherPort;
import com.ecommerce.inventory.domain.event.InventoryReleasedEvent;
import com.ecommerce.inventory.domain.event.InventoryReservedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class InventoryEventPublisherAdapter implements InventoryEventPublisherPort {

    private static final Logger logger = LoggerFactory.getLogger(InventoryEventPublisherAdapter.class);
    private static final String INVENTORY_EVENTS_TOPIC = "inventory.events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final ObservationRegistry observationRegistry;

    public InventoryEventPublisherAdapter(KafkaTemplate<String, String> kafkaTemplate,
                                      ObjectMapper objectMapper,
                                      ObservationRegistry observationRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.observationRegistry = observationRegistry;

        // Configurar observação no KafkaTemplate
        this.kafkaTemplate.setObservationEnabled(true);
    }

    @Override
    public void publishInventoryReserved(InventoryReservedEvent event) {
        publishEvent(event, "InventoryReserved");
    }

    @Override
    public void publishInventoryReleased(InventoryReleasedEvent event) {
        publishEvent(event, "InventoryReleased");
    }

    private void publishEvent(Object event, String eventType) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            String key = extractKeyFromEvent(event);

            CompletableFuture<SendResult<String, String>> future = 
                kafkaTemplate.send(INVENTORY_EVENTS_TOPIC, key, eventJson);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("{} event published successfully. Topic: {}, Key: {}, Offset: {}", 
                        eventType, INVENTORY_EVENTS_TOPIC, key, result.getRecordMetadata().offset());
                } else {
                    logger.error("Failed to publish {} event. Topic: {}, Key: {}", 
                        eventType, INVENTORY_EVENTS_TOPIC, key, ex);
                }
            });

        } catch (JsonProcessingException ex) {
            logger.error("Failed to serialize {} event", eventType, ex);
            throw new RuntimeException("Failed to publish event", ex);
        }
    }

    private String extractKeyFromEvent(Object event) {
        if (event instanceof InventoryReservedEvent reservedEvent) {
            return reservedEvent.orderId().toString();
        } else if (event instanceof InventoryReleasedEvent releasedEvent) {
            return releasedEvent.orderId().toString();
        }
        return "unknown";
    }
}