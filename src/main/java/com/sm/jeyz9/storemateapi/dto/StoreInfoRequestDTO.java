package com.sm.jeyz9.storemateapi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StoreInfoRequestDTO {

    private String storeName;

    private LocalTime openTime;
    private LocalTime closeTime;
    private String phone;
    private String streetAddress;

    @Email(message = "Invalid email format")
    private String email;
}