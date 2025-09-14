package com.ecommerce.billing.application.service;

import com.ecommerce.billing.application.port.in.GetPaymentByOrderQuery;
import com.ecommerce.billing.application.port.in.GetPaymentByOrderUseCase;
import com.ecommerce.billing.application.port.in.GetPaymentResponse;
import com.ecommerce.billing.application.port.out.PaymentRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GetPaymentByOrderService implements GetPaymentByOrderUseCase {

    private static final Logger logger = LoggerFactory.getLogger(GetPaymentByOrderService.class);

    private final PaymentRepositoryPort paymentRepository;

    public GetPaymentByOrderService(PaymentRepositoryPort paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public Optional<GetPaymentResponse> execute(GetPaymentByOrderQuery query) {
        logger.info("Getting payment for order: {}", query.orderId());
        
        var paymentOpt = paymentRepository.findByOrderId(query.orderId());
        
        if (paymentOpt.isEmpty()) {
            logger.warn("Payment not found for order: {}", query.orderId());
            return Optional.empty();
        }
        
        var payment = paymentOpt.get();
        var response = new GetPaymentResponse(
            payment.getId(),
            payment.getOrderId(),
            payment.getPaymentReference(),
            payment.getAmount(),
            payment.getCurrency(),
            payment.getStatus().name(),
            payment.getPaymentMethod().name(),
            payment.getProcessedAt()
        );
        
        logger.info("Found payment for order: {}", query.orderId());
        return Optional.of(response);
    }
}