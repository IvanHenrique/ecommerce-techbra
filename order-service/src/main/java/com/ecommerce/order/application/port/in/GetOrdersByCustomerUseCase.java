package com.ecommerce.order.application.port.in;

import java.util.List;

public interface GetOrdersByCustomerUseCase {

    List<GetOrderResponse> execute(GetOrdersByCustomerQuery query);
}