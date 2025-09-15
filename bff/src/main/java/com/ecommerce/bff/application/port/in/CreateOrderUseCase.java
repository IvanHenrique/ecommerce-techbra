package com.ecommerce.bff.application.port.in;

import com.ecommerce.shared.domain.common.Result;

public interface CreateOrderUseCase {
    
    Result<CreateOrderResponse> execute(CreateOrderCommand command);
}