// In RiskRepository.java
package com.cap.api.service.riskapp.repository;

import com.cap.api.service.riskapp.model.Risk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RiskRepository extends JpaRepository<Risk, Integer> {

}