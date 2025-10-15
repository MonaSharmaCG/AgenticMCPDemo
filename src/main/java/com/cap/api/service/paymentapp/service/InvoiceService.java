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
        var invoiceList =  invoiceRepository.findUnpaidInvoicesForClient(clientId);
        return  invoiceList;
    }
}