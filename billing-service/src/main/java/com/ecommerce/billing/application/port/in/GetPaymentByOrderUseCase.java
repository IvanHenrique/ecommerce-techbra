package com.ecommerce.billing.application.port.in;

import java.util.Optional;

public interface GetPaymentByOrderUseCase {
    
    Optional<GetPaymentResponse> execute(GetPaymentByOrderQuery query);
}