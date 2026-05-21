package com.sm.jeyz9.storemateapi.services;

import com.sm.jeyz9.storemateapi.dto.OwnerDashboardDTO;
import com.sm.jeyz9.storemateapi.dto.SalesAnalyticsDashboardDTO;
import com.sm.jeyz9.storemateapi.repository.OwnerDashboardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OwnerDashboardService {
    private final OwnerDashboardRepository ownerDashboardRepository;
    
    @Autowired
    public OwnerDashboardService(OwnerDashboardRepository ownerDashboardRepository) {
        this.ownerDashboardRepository = ownerDashboardRepository;
    }
    
    public OwnerDashboardDTO getAdminDashboard() {
        return ownerDashboardRepository.findAdminDashboard().orElse(new OwnerDashboardDTO());
    }
    
    public SalesAnalyticsDashboardDTO salesAnalyticsDashboard() {
        return ownerDashboardRepository.findSalesAnalyticsDashboard("").orElse(new SalesAnalyticsDashboardDTO());
    }
}
