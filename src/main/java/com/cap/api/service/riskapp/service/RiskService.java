// In RiskService.java
package com.cap.api.service.riskapp.service;

import com.cap.api.service.riskapp.model.Risk;
import com.cap.api.service.riskapp.repository.RiskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class RiskService {
    @Autowired
    private RiskRepository riskRepository;

    // TODO getRiskFactor method implementation.
    public double getRiskFactor(int clientId) {
    Risk risk = riskRepository.findInvoiceCountAndTotalByClientId(clientId);
    var invoiceTotal = risk.getInvoiceTotal().doubleValue();
    return invoiceTotal / risk.getInvoiceCount() * 5;
   }
}