// In RiskRepository.java
package com.cap.api.service.riskapp.repository;

import com.cap.api.service.riskapp.model.Risk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RiskRepository extends JpaRepository<Risk, Integer> {

    @Query("SELECT new com.cap.api.service.riskapp.model.Risk(i.clientId, COUNT(i), SUM(i.invoiceTotal)) FROM Invoice i WHERE i.clientId = :clientId GROUP BY i.clientId")
    Risk findInvoiceCountAndTotalByClientId(@Param("clientId") int clientId);
}