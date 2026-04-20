package com.sm.jeyz9.storemateapi.dto;

import com.sm.jeyz9.storemateapi.models.CheckoutTypeName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CheckoutNowRequestDTO {
    private Long id;
    private Integer quantity;
    private CheckoutTypeName checkoutType;
}
