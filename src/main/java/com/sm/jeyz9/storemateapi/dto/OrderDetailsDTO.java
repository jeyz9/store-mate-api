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
public class OrderDetailsDTO {
    private Long id;
    private String orderNo;
    private String status;
    private List<OrderItemDTO> orderItems;
    private OrderRecipientDTO orderRecipient;
    private Double total;
    private String checkoutType;
    private String reason;
}
