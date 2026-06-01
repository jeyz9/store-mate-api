package com.sm.jeyz9.storemateapi.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductImportDTO {
    private Long productId;
    private String productName;
    private Integer quantity;
}
