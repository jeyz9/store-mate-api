package com.sm.jeyz9.storemateapi.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReviewRequestDTO {
    @NotNull(message = "Review score is required")
    @Min(value = 1, message = "Score must be at least 1")
    @Max(value = 5, message = "Score must not exceed 5")
    private Float reviewScore;

    private String message;
}