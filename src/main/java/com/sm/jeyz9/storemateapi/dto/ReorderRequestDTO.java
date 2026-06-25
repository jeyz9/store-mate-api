package com.sm.jeyz9.storemateapi.dto;

import com.sm.jeyz9.storemateapi.models.CheckoutTypeName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReorderRequestDTO {
    private String orderNo;
    private CheckoutTypeName checkoutType;
}
