package com.cap.api.service.repository;

import com.cap.api.service.entity.Claim;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimRepository extends JpaRepository<Claim, Integer> {
}