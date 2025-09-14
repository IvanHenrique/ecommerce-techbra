package com.ecommerce.inventory.application.port.in;

import java.util.List;

public interface GetReservationsByOrderUseCase {

    List<GetReservationResponse> execute(GetReservationsByOrderQuery query);
}