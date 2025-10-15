package com.cap.api.service.riskapp.controller;


import com.cap.api.service.riskapp.service.RiskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/risk")
public class RiskController {

    @Autowired
    private RiskService riskService;

    @GetMapping("/factor")
    public ResponseEntity<Double> getRiskFactor(@RequestParam int clientId) {
        double riskFactor = riskService.getRiskFactor(clientId);
        return ResponseEntity.ok(riskFactor);
    }
}