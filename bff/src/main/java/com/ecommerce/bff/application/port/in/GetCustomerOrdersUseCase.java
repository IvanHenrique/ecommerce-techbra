package com.ecommerce.bff.application.port.in;

import com.ecommerce.bff.domain.view.OrderSummaryView;

import java.util.List;

public interface GetCustomerOrdersUseCase {

    List<OrderSummaryView> execute(GetCustomerOrdersQuery query);
}