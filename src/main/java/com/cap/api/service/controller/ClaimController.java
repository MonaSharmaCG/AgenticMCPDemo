package com.cap.api.service.controller;

import com.cap.api.service.entity.Claim;
import com.cap.api.service.service.ClaimService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/claims")
public class ClaimController {

    @Autowired
    private ClaimService claimService;

    @PutMapping("/approve/{claimId}")
    public String approveClaim(@PathVariable int claimId, @RequestParam String approvedBy) {
        return claimService.approveClaim(claimId, approvedBy);
    }

   //public method to insert a claim
    @PostMapping("/add")
    public ResponseEntity<?> insertClaim(@RequestBody Claim claim) {
       
            Claim savedClaim = claimService.insertClaim(claim);
            return ResponseEntity.ok(savedClaim);
       
    }

    //public method to get all claims
    @GetMapping("/all")
    public List<Claim> getAllClaims() {
        return claimService.getAllClaims();
    }

    //public method to get a claim by id
    @GetMapping("/{claimId}")
    public Claim getClaimById(@PathVariable int claimId) {
        return claimService.getClaimById(claimId);
    }
}