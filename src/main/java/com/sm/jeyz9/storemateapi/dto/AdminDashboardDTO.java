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
public class AdminDashboardDTO {
    private Integer activeUsers;
    private Integer newUsers;
    private List<ActiveUserChartDTO> weeklyActiveUsersChart;
    private List<LatestOrderDTO> latestOrder;
    private List<OrderChannelRateDTO> orderChannelRate;
    private List<RegionalRevenueDTO> regionalRevenue;
    private List<ProductDashboardDTO> products;
    private List<ReviewDashboardDTO> reviews;
}
