package com.ecommerce.shared.domain.event;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.UUID;

public record DomainEvent(
        UUID eventId,
        String eventType,
        UUID aggregateId,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime occurredOn,
        Integer version
) {
    public static DomainEvent create(String eventType, UUID aggregateId) {
        return new DomainEvent(
                UUID.randomUUID(),
                eventType,
                aggregateId,
                LocalDateTime.now(),
                1
        );
    }
}