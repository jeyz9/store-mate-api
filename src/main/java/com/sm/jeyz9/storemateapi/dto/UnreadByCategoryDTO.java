package com.sm.jeyz9.storemateapi.dto;

import com.sm.jeyz9.storemateapi.models.NotifyTypeName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UnreadByCategoryDTO {
    private NotifyTypeName notifyType;
    private Integer unread;
}
