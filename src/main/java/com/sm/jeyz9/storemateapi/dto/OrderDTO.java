package com.sm.jeyz9.storemateapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderDTO {
    private Long id;
    private String orderNo;
    private String status;
    private String checkoutType;
    private List<OrderAddressDTO> orderAddress;
    private List<OrderItemDTO> orderItems;
    private Double total;
    private LocalDateTime paidAt;
}
