package com.cap.api.service.riskapp.model;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.math.BigDecimal;

@Entity
public class Risk {
    @Id
    private int clientId;
    private long invoiceCount;
    private BigDecimal invoiceTotal;

    public Risk(Integer clientId, Long invoiceCount, BigDecimal invoiceTotal) {
        this.clientId = clientId;
        this.invoiceCount = invoiceCount;
        this.invoiceTotal = invoiceTotal;
    }
    // Getters and Setters
    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public long getInvoiceCount() {
        return invoiceCount;
    }

    public void setInvoiceCount(long invoiceCount) {
        this.invoiceCount = invoiceCount;
    }

    public BigDecimal getInvoiceTotal() {
        return invoiceTotal;
    }

    public void setInvoiceTotal(BigDecimal invoiceTotal) {
        this.invoiceTotal = invoiceTotal;
    }
}