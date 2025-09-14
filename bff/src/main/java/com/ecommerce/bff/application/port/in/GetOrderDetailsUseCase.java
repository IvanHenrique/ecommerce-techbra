package com.ecommerce.bff.application.port.in;

import com.ecommerce.bff.domain.view.CustomerOrderView;

import java.util.Optional;

public interface GetOrderDetailsUseCase {
    
    Optional<CustomerOrderView> execute(GetOrderDetailsQuery query);
}