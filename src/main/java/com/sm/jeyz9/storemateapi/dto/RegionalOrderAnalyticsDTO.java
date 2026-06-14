package com.sm.jeyz9.storemateapi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RegionalOrderAnalyticsDTO {
    private String geography;
    private Long totalOrder;
    private BigDecimal totalRevenue;
    private Long totalUser;
}
