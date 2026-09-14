package com.sm.jeyz9.storemateapi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class YearActiveIncomeChartDTO {
    private BigDecimal growthRate;
    private YearActiveGraphDTO thisYear;
    private YearActiveGraphDTO lastYear;
}
