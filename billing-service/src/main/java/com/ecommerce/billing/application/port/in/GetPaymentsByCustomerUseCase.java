package com.ecommerce.billing.application.port.in;

import java.util.List;

public interface GetPaymentsByCustomerUseCase {

    List<GetPaymentResponse> execute(GetPaymentsByCustomerQuery query);
}