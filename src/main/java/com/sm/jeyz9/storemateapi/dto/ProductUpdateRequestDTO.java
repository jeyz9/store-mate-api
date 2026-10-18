package com.sm.jeyz9.storemateapi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductUpdateRequestDTO {
    @NotBlank(message = "Product name is required")
    private String productName;
    private Long categoryId;
    private Double price;
    private Long statusId;
    private String description;
    private Integer stockQuantity;
    
    private List<Long> removeImages;
}
