package com.sm.jeyz9.storemateapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserManagementDTO {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String role;
    private boolean isSuspended;
    private String suspensionReason;
    private LocalDateTime suspendAt;
}