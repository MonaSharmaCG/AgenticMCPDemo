package com.cap.api.service.paymentapp.repository;



import com.cap.api.service.paymentapp.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Integer> {

    @Modifying
    @Transactional
    @Query("UPDATE Invoice i SET i.paymentTotal = :paymentTotal, i.paymentDate = :paymentDate WHERE i.invoiceId = :invoiceId")
    void updatePaymentDetails(@Param("invoiceId") int invoiceId, @Param("paymentTotal") BigDecimal paymentTotal, @Param("paymentDate") LocalDate paymentDate);

    @Query("SELECT i FROM Invoice i WHERE i.clientId = :clientId AND i.paymentTotal = 0")
    List<Invoice> findUnpaidInvoicesForClient(@Param("clientId") int clientId);


}