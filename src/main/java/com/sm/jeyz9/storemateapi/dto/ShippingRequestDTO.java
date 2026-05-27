package com.sm.jeyz9.storemateapi.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ShippingRequestDTO {
    @NotNull(message = "Ids is required")
    List<Long> ids;
}
