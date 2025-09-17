package com.ecommerce.bff.infrastructure.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class ManualCacheService {

    private static final Logger logger = LoggerFactory.getLogger(ManualCacheService.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public ManualCacheService(
            RedisTemplate<String, String> redisTemplate,
            @Qualifier("redisObjectMapper") ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public <T> void put(String key, T value, Duration ttl) {
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, jsonValue, ttl);
            logger.debug("L2 cache stored for key: {}", key);
        } catch (Exception e) {
            logger.error("Failed to store L2 cache for key: {}", key, e);
        }
    }

    public <T> Optional<T> get(String key, TypeReference<T> typeRef) {
        try {
            String jsonValue = redisTemplate.opsForValue().get(key);
            if (jsonValue != null) {
                T value = objectMapper.readValue(jsonValue, typeRef);
                logger.debug("L2 cache hit for key: {}", key);
                return Optional.of(value);
            }
            logger.debug("L2 cache miss for key: {}", key);
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Failed to get L2 cache for key: {}", key, e);
            return Optional.empty();
        }
    }

    public void evict(String key) {
        try {
            redisTemplate.delete(key);
            logger.debug("L2 cache evicted for key: {}", key);
        } catch (Exception e) {
            logger.error("Failed to evict L2 cache for key: {}", key, e);
        }
    }
}