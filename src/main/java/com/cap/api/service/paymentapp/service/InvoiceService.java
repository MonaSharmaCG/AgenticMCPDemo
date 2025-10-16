package com.cap.api.service.paymentapp.service;



import com.cap.api.service.paymentapp.model.Invoice;
import com.cap.api.service.paymentapp.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    public List<Invoice> getUnpaidInvoicesForClients(int clientId) {
        var allInvoices = invoiceRepository.findAll();
        return allInvoices.stream()
            .filter(i -> i.getClientId() == clientId && i.getPaymentTotal().compareTo(java.math.BigDecimal.ZERO) == 0)
            .toList();
    }
}