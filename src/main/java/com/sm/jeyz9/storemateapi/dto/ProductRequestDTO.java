package com.sm.jeyz9.storemateapi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductRequestDTO {
    private String productName;
    private Long categoryId;
    private Double price;
    private Long statusId;
    private String description;
    private Integer stockQuantity;
}
