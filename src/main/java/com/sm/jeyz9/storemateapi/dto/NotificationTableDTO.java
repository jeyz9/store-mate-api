package com.sm.jeyz9.storemateapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NotificationTableDTO {
    private List<NotifyResponseDTO> notifyList;
    private int totalUnread;
    private UnreadByCategoryDTO unreadByCategory;
}
