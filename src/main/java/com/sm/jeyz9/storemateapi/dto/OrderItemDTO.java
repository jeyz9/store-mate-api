package com.sm.jeyz9.storemateapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class OrderItemDTO {
    private Long id;
    private String productName;
    private String imageUrl;
    private Double price;
    private Integer quantity;
    private Double subTotal;
    
    @JsonProperty("is_review")
    private boolean review;
}
