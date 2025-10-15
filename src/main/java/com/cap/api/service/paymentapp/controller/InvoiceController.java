package com.cap.api.service.paymentapp.controller;



import com.cap.api.service.paymentapp.model.Invoice;
import com.cap.api.service.paymentapp.model.PaymentRequest;
import com.cap.api.service.paymentapp.service.InvoiceService;
import com.cap.api.service.paymentapp.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class InvoiceController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private InvoiceService invoiceService;



    @PostMapping("/makePayment")
    public ResponseEntity<String> makePayment(@RequestBody PaymentRequest paymentRequest) {
        paymentService.makeValidatePayments(paymentRequest.getInvoiceId(), paymentRequest.getPaymentTotal(), LocalDate.parse(paymentRequest.getPaymentDate()));
        return ResponseEntity.ok("Payment made successfully");
    }

    @GetMapping("/unpaidInvoices")
    public ResponseEntity<List<Invoice>> getUnpaidInvoices(@RequestParam int clientId) {
        List<Invoice> invoices = invoiceService.getUnpaidInvoicesForClients(clientId);
        return new ResponseEntity<>(invoices, HttpStatus.OK);
    }


}
