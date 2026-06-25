package com.sm.jeyz9.storemateapi.dto;

import com.sm.jeyz9.storemateapi.models.NotifyTypeName;
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
public class NotificationDTO {
    private String title;
    private String message;
    private NotifyTypeName notifyType;
}
