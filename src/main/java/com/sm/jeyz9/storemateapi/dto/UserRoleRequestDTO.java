package com.sm.jeyz9.storemateapi.dto;

import com.sm.jeyz9.storemateapi.models.RoleName;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserRoleRequestDTO {

    @NotNull(message = "Role is required")
    private RoleName roleName;
}