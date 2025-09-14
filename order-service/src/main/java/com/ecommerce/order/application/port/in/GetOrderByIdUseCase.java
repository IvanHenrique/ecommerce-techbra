package com.ecommerce.order.application.port.in;

import java.util.Optional;

public interface GetOrderByIdUseCase {
    
    Optional<GetOrderResponse> execute(GetOrderByIdQuery query);
}