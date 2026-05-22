package com.sm.jeyz9.storemateapi.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ShippingImportDTO {
    private String receiverName;
    private String phoneNumber;
    private String address;
    private String zipcode;
    private List<ProductImportDTO> products;
    private Boolean returned;
}
