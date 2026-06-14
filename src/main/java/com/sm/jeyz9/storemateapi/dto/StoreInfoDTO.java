package com.sm.jeyz9.storemateapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class StoreInfoDTO {
    private Long id;
    private String storeName;
    private String phone;
    private String streetAddress;
    private String subdistrict;
    private String district;
    private String province;
    private String zipcode;
    private String email;
    private String promotionImage;
}