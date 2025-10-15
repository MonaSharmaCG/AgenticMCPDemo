package com.cap.api.service.paymentapp.model;

import java.math.BigDecimal;

public class PaymentRequest {
    private int invoiceId;
    private BigDecimal paymentTotal;
    private String paymentDate;

    // Getters and setters
    public int getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(int invoiceId) {
        this.invoiceId = invoiceId;
    }

    public BigDecimal getPaymentTotal() {
        return paymentTotal;
    }

    public void setPaymentTotal(BigDecimal paymentTotal) {
        this.paymentTotal = paymentTotal;
    }

    public String getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(String paymentDate) {
        this.paymentDate = paymentDate;
    }
}