package com.sm.jeyz9.storemateapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAddressDTO {
    private Long id;
    private String receiverName;
    private String receiverPhone;
    private String streetAddress;
    private String subdistrict;
    private String district;
    private String province;
    private String zipcode;
    private Boolean isDefault;
}