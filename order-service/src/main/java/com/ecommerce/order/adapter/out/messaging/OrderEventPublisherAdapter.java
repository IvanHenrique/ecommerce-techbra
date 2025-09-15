package com.ecommerce.order.adapter.out.messaging;

import com.ecommerce.order.application.port.out.OrderEventPublisherPort;
import com.ecommerce.order.domain.event.OrderCreatedEvent;
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
public class OrderEventPublisherAdapter implements OrderEventPublisherPort {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventPublisherAdapter.class);
    private static final String ORDER_EVENTS_TOPIC = "order.events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final ObservationRegistry observationRegistry;

    public OrderEventPublisherAdapter(KafkaTemplate<String, String> kafkaTemplate,
                                      ObjectMapper objectMapper,
                                      ObservationRegistry observationRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.observationRegistry = observationRegistry;

        // Configurar observação no KafkaTemplate
        this.kafkaTemplate.setObservationEnabled(true);
    }

    @Override
    public void publishOrderCreated(OrderCreatedEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            String key = event.aggregateId().toString();

            CompletableFuture<SendResult<String, String>> future = 
                kafkaTemplate.send(ORDER_EVENTS_TOPIC, key, eventJson);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("OrderCreated event published successfully. Topic: {}, Key: {}, Offset: {}", 
                        ORDER_EVENTS_TOPIC, key, result.getRecordMetadata().offset());
                } else {
                    logger.error("Failed to publish OrderCreated event. Topic: {}, Key: {}", 
                        ORDER_EVENTS_TOPIC, key, ex);
                }
            });

        } catch (JsonProcessingException ex) {
            logger.error("Failed to serialize OrderCreated event", ex);
            throw new RuntimeException("Failed to publish event", ex);
        }
    }
}