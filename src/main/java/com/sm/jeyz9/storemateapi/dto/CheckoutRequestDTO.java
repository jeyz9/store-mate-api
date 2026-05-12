package com.sm.jeyz9.storemateapi.dto;

import com.sm.jeyz9.storemateapi.models.CheckoutTypeName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CheckoutRequestDTO {
    private List<Long> ids;
    private CheckoutTypeName checkoutType;
}
