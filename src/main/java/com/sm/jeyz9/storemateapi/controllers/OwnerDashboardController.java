package com.sm.jeyz9.storemateapi.controllers;

import com.sm.jeyz9.storemateapi.dto.OwnerDashboardDTO;
import com.sm.jeyz9.storemateapi.dto.SalesAnalyticsDashboardDTO;
import com.sm.jeyz9.storemateapi.services.OwnerDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/owner")
public class OwnerDashboardController {
    private final OwnerDashboardService ownerDashboardService;
    
    @Autowired
    public OwnerDashboardController(OwnerDashboardService ownerDashboardService) {
        this.ownerDashboardService = ownerDashboardService;
    }
    
    @GetMapping("/dashboard")
    public ResponseEntity<OwnerDashboardDTO> getAdminDashboard() {
        return new ResponseEntity<>(ownerDashboardService.getAdminDashboard(), HttpStatus.OK);
    }
    
    @GetMapping("/sales-analytics/dashboard")
    public ResponseEntity<SalesAnalyticsDashboardDTO> salesAnalyticsDashboard() {
        return new ResponseEntity<>(ownerDashboardService.salesAnalyticsDashboard(), HttpStatus.OK);
    }
}
