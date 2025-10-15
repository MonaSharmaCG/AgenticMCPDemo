package com.cap.api.service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDate;

@Entity
public class Claim {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int claimId;
    private String claimReason;
    private String claimType;
    private double claimAmount;
    private LocalDate claimDate;
    private String claimStatus;
    
    public int getClaimId() {
        return claimId;
    }
    
    public void setClaimId(int claimId) {
        this.claimId = claimId;
    }
    
    public String getClaimReason() {
        return claimReason;
    }
    
    public void setClaimReason(String claimReason) {
        this.claimReason = claimReason;
    }
    
    public String getClaimType() {
        return claimType;
    }
    
    public void setClaimType(String claimType) {
        this.claimType = claimType;
    }
    
    public double getClaimAmount() {
        return claimAmount;
    }
    
    public void setClaimAmount(double claimAmount) {
        this.claimAmount = claimAmount;
    }
    
    public LocalDate getClaimDate() {
        return claimDate;
    }
    
    public void setClaimDate(LocalDate claimDate) {
        this.claimDate = claimDate;
    }
    
    public String getClaimStatus() {
        return claimStatus;
    }
    
    public void setClaimStatus(String claimStatus) {
        this.claimStatus = claimStatus;
    }
}