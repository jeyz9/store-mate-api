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
public class UserAddressRequestDTO {
    private String streetAddress; 
    private Long subdistrictId;   
    private String postalCode;    
    private Boolean isDefault;    
}