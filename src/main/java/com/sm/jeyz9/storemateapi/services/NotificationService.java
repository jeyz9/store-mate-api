package com.sm.jeyz9.storemateapi.services;

import com.sm.jeyz9.storemateapi.dto.NotifyOwnerResponseDTO;
import com.sm.jeyz9.storemateapi.dto.NotifyRequestDTO;
import com.sm.jeyz9.storemateapi.dto.NotifyResponseDTO;

import java.util.List;

public interface NotificationService {
    String sendNotification(String email, NotifyRequestDTO request);
    List<NotifyResponseDTO> getAllNotifyUser(String email);
    List<NotifyOwnerResponseDTO> getAllNotify();
    String removeNotify(Long notifyId);
}
