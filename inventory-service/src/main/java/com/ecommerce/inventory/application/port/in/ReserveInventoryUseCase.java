package com.ecommerce.inventory.application.port.in;

import com.ecommerce.shared.domain.common.Result;

public interface ReserveInventoryUseCase {
    
    Result<ReserveInventoryResponse> execute(ReserveInventoryCommand command);
}