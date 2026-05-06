package com.sm.jeyz9.storemateapi.dto;

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
public class OrderRecipientDTO {
    private String recipientName;
    private String phone;
    private String streetAddress;
    private String subdistrict;
    private String district;
    private String province;
    private String zipcode;
}
