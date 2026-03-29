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
public class OrderDTO {
    private Long id;
    private String status;
    private OrderAddressDTO orderAddress;
    private List<OrderItemDTO> orderItems;
    private String stripePaymentIntent;
    private Double total;
}
