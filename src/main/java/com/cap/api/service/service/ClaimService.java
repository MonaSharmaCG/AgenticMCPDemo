package com.cap.api.service.service;

import com.cap.api.service.entity.Claim;
import com.cap.api.service.paymentapp.model.Invoice;
import com.cap.api.service.paymentapp.service.InvoiceService;
import com.cap.api.service.repository.ClaimRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClaimService {

    @Autowired
    private ClaimRepository claimRepository;
    @Autowired
    private InvoiceService invoiceService;


    //write a method to insert a claim
    public Claim insertClaim(Claim claim) {
        // basic validation: ensure claim date is present and not in the future
        String dateValidation = validateClaimDate(claim);
        if (dateValidation != null) {
            throw new RuntimeException("Claim validation failed: " + dateValidation);
        }
        return claimRepository.save(claim);
    }

    // validate claim date - returns null when valid, otherwise an error message
    public String validateClaimDate(Claim claim) {
        if (claim == null) return "claim is null";
        java.time.LocalDate claimDate = claim.getClaimDate();
        if (claimDate == null) return "claim date is required";
        if (claimDate.isAfter(java.time.LocalDate.now())) return "claim date cannot be in the future";
        return null;
    }

    //write a method to get all claims
    public List<Claim> getAllClaims() {
        return claimRepository.findAll();
    }



    //write a method to get a claim by id
    public Claim getClaimById(int claimId) {
        return claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim ID not found."));
    }

    public String approveClaim(int claimId, String approvedBy) {
        List<Invoice> unpaidInvoices = invoiceService.getUnpaidInvoicesForClients(claimId);

        if (unpaidInvoices.size() > 1) {
            return "You have more than 1 unpaid premium. Max allowed unpaid invoice is 1 for approving your upcoming claims";
        }
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim ID not found."));

        if (claim.getClaimStatus() != null) {
            return "Claim " + claimId + " has already been " + claim.getClaimStatus() + ".";
        } else {
            claim.setClaimStatus("Approved");
            claimRepository.save(claim);
            return "Claim " + claimId + " approved successfully by " + approvedBy + ".";
        }
    }
}