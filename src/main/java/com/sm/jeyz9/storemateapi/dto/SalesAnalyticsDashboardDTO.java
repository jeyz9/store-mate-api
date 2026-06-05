package com.sm.jeyz9.storemateapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SalesAnalyticsDashboardDTO {
    private BigDecimal totalPrice;
    private Long totalOrder;
    private List<OrderChannelIncomeDTO> orderChannelIncome;
    private List<RegionalOrderAnalyticsDTO> regionalOrders;
    private List<RegionalRevenueAnalyticsDTO> regionalRevenue;
    private List<RegionalUserAnalyticsDTO> regionalUsers;
}
