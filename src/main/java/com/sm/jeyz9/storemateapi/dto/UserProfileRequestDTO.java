package com.sm.jeyz9.storemateapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
public class UserProfileRequestDTO {
    private String name;
    private String email;
    private String phone;
    
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String imageUrl;
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String createdAt; 
}