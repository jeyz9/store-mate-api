package com.sm.jeyz9.storemateapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderModDTO {
    private Long id;
    private String orderNo;
    private String recipientName;
    private String phone;
    private LocalDateTime createdAt;
    private Double total;
    private String shippingFrom;
    private String status;
    
    @JsonProperty("is_printed")
    private boolean printed;
}
