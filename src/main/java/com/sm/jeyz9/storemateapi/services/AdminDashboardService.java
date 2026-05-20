package com.sm.jeyz9.storemateapi.services;

import com.sm.jeyz9.storemateapi.dto.AdminDashboardDTO;
import com.sm.jeyz9.storemateapi.repository.AdminDashboardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AdminDashboardService {
    private final AdminDashboardRepository adminDashboardRepository;
    
    @Autowired
    public AdminDashboardService(AdminDashboardRepository adminDashboardRepository) {
        this.adminDashboardRepository = adminDashboardRepository;
    }
    
    public AdminDashboardDTO getAdminDashboard() {
        return adminDashboardRepository.findAdminDashboard().orElse(null);
    }
}
