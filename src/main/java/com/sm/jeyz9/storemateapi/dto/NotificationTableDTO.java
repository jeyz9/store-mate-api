package com.sm.jeyz9.storemateapi.dto;

import com.sm.jeyz9.storemateapi.models.NotifyTypeName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NotificationTableDTO {
    private List<NotifyResponseDTO> notifyList;
    private int totalUnread;
    private Map<NotifyTypeName, Integer> unreadByCategory;
}
