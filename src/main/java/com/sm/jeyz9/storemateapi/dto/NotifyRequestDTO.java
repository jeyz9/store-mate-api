package com.sm.jeyz9.storemateapi.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NotifyRequestDTO {
    @NotBlank(message = "Title is required!")
    private String title;
    
    @NotBlank(message = "Message is required!")
    private String message;
    
    @NotBlank(message = "SendTo to is required!")
    private String sendTo;
}
