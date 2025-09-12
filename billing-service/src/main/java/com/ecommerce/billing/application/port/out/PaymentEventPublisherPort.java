package com.ecommerce.billing.application.port.out;

import com.ecommerce.billing.domain.event.PaymentCompletedEvent;
import com.ecommerce.billing.domain.event.PaymentFailedEvent;

public interface PaymentEventPublisherPort {
    
    void publishPaymentCompleted(PaymentCompletedEvent event);
    
    void publishPaymentFailed(PaymentFailedEvent event);
}