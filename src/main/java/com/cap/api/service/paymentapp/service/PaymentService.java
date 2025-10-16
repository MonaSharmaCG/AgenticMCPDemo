package com.cap.api.service.paymentapp.service;


import com.cap.api.service.paymentapp.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class PaymentService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    public void makeValidatePayments(int invoiceId, BigDecimal paymentTotal, LocalDate paymentDate) {
        if (paymentTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid Payment Amount");
        }
        // Update logic removed for troubleshooting
    }
}