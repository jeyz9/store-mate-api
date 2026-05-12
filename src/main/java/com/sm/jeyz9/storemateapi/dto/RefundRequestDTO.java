package com.sm.jeyz9.storemateapi.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RefundRequestDTO {
    @NotBlank(message = "Order No is require!")
    private String orderNo;
    
    @NotBlank(message = "Reason is require!")
    private String reason;
    
    private String description;
}
