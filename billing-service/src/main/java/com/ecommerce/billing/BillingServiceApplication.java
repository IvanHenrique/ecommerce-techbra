package com.ecommerce.billing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication(scanBasePackages = {
        "com.ecommerce.billing",
        "com.ecommerce.shared.infrastructure"
})
@EnableKafka
@EnableCaching
@EntityScan(basePackages = {
        "com.ecommerce.billing.domain.model",
        "com.ecommerce.shared.domain.entity"
})
@EnableJpaRepositories(basePackages = "com.ecommerce.billing.adapter.out.persistence")
public class BillingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BillingServiceApplication.class, args);
    }
}