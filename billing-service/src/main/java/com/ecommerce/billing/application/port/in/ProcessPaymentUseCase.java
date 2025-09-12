package com.ecommerce.billing.application.port.in;

import com.ecommerce.shared.domain.common.Result;

public interface ProcessPaymentUseCase {
    
    Result<ProcessPaymentResponse> execute(ProcessPaymentCommand command);
}