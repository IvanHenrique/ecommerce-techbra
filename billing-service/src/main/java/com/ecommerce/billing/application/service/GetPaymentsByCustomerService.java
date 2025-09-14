package com.ecommerce.billing.application.service;

import com.ecommerce.billing.application.port.in.GetPaymentResponse;
import com.ecommerce.billing.application.port.in.GetPaymentsByCustomerQuery;
import com.ecommerce.billing.application.port.in.GetPaymentsByCustomerUseCase;
import com.ecommerce.billing.application.port.out.PaymentRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetPaymentsByCustomerService implements GetPaymentsByCustomerUseCase {

    private static final Logger logger = LoggerFactory.getLogger(GetPaymentsByCustomerService.class);

    private final PaymentRepositoryPort paymentRepository;

    public GetPaymentsByCustomerService(PaymentRepositoryPort paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public List<GetPaymentResponse> execute(GetPaymentsByCustomerQuery query) {
        logger.info("Getting payments for customer: {}", query.customerId());
        
        var payments = paymentRepository.findByCustomerId(query.customerId());
        
        var response = payments.stream()
            .map(payment -> new GetPaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getPaymentReference(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus().name(),
                payment.getPaymentMethod().name(),
                payment.getProcessedAt()
            ))
            .toList();
        
        logger.info("Found {} payments for customer: {}", response.size(), query.customerId());
        return response;
    }
}