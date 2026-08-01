package com.sm.jeyz9.storemateapi.services;

import com.sm.jeyz9.storemateapi.dto.NotificationTableDTO;
import com.sm.jeyz9.storemateapi.dto.NotifyOwnerResponseDTO;
import com.sm.jeyz9.storemateapi.dto.NotifyRequestDTO;
import com.sm.jeyz9.storemateapi.dto.NotifyResponseDTO;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface NotificationService {
    String sendNotification(String email, NotifyRequestDTO request);
    Optional<NotificationTableDTO> getAllNotifyUser(String email, String type);
    Page<NotifyOwnerResponseDTO> getAllNotify(String keyword, Integer page, Integer size);
    String removeNotify(Long notifyId);
    
    String markAsRead(String email, Long notifyId);
    String markAllAsRead(String email);
}
