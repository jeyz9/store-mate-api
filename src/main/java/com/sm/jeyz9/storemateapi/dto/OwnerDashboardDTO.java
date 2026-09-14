package com.sm.jeyz9.storemateapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OwnerDashboardDTO {
    private Integer activeUsers;
    private Integer newUserToday;
    private Integer totalRevenue;
    private Integer totalOrder;
    private Integer newUsers;
    private Integer totalProductSale;
    private YearActiveIncomeChartDTO yearActiveIncomeChart;
    private WeeklyActiveIncomeChartDTO weeklyActiveIncomeChart;
    private List<YearActiveOrderChartDTO> yearActiveOrderChart;
    private List<OrderChannelReteDTO> orderChannelRete;
    private List<SalesPercentageDTO> salesPercentage;
    private UserChartDTO userChart;
    private List<RegionalRevenueDTO> regionalRevenue;
    private List<ReviewDashboardDTO> reviews;
    private List<ProductAlertDTO> productAlert;
}
