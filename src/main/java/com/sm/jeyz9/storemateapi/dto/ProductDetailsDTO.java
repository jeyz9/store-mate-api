package com.sm.jeyz9.storemateapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDetailsDTO {
    private Long id;
    private String productName;
    private String description;
    private Integer quantity;
    private Float RatingScore;
    private List<ProductImageDTO> productImages;
    private Double price;
    private List<ReviewDTO> reviews;
    private String productStatus;
}
