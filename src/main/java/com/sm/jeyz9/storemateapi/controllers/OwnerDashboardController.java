package com.sm.jeyz9.storemateapi.controllers;

import com.sm.jeyz9.storemateapi.dto.OwnerDashboardDTO;
import com.sm.jeyz9.storemateapi.dto.SalesAnalyticsDashboardDTO;
import com.sm.jeyz9.storemateapi.services.OwnerDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/owner")
public class OwnerDashboardController {
    private final OwnerDashboardService ownerDashboardService;
    
    @Autowired
    public OwnerDashboardController(OwnerDashboardService ownerDashboardService) {
        this.ownerDashboardService = ownerDashboardService;
    }
    
    @GetMapping("/dashboard")
    public ResponseEntity<OwnerDashboardDTO> getAdminDashboard(
            @RequestParam(defaultValue = "YEAR") String salesPeriod,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate salesDate,
            @RequestParam(defaultValue = "MONTH") String comparePeriod,
            @RequestParam(required = false) Integer compareFromYear,
            @RequestParam(required = false) Integer compareToYear
    ) {
        return new ResponseEntity<>(ownerDashboardService.getAdminDashboard(
                salesPeriod, salesDate, comparePeriod, compareFromYear, compareToYear
        ), HttpStatus.OK);
    }
    
    @Operation(
            description = """
             Period
                TODAY: วันนี้
                WEEK: สัปดาห์นี้
                MONTH: เดือนนี้
            """
    )
    @GetMapping("/sales-analytics/dashboard")
    public ResponseEntity<SalesAnalyticsDashboardDTO> salesAnalyticsDashboard(@RequestParam(required = false, defaultValue = "MONTH") String period) {
        return new ResponseEntity<>(ownerDashboardService.salesAnalyticsDashboard(period), HttpStatus.OK);
    }

    @PostMapping("/import")
    public ResponseEntity<String> importExcel(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ownerDashboardService.importExcel(file));
    }
}
