package com.sm.jeyz9.storemateapi.dto;

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
public class RefundDetailsDTO {
    private String refundNo;
    private String orderNo;
    private String receiverName;
    private Double total;
    private String reason;
    private LocalDateTime requestedAt;
    private String status;
}
