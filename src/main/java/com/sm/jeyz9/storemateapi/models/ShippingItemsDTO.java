package com.sm.jeyz9.storemateapi.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShippingItemsDTO {
    private String productName;
    private int quantity;
    private Double price;
}
