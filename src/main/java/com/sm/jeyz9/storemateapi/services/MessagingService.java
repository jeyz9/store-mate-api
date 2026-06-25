package com.sm.jeyz9.storemateapi.services;

import com.sm.jeyz9.storemateapi.dto.NotifyRequestDTO;
import com.sm.jeyz9.storemateapi.dto.NotifyResponseDTO;
import com.sm.jeyz9.storemateapi.models.Notification;
import com.sm.jeyz9.storemateapi.models.SendTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class MessagingService {
    private final SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    public MessagingService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendToUser(String email, String status) {
        if (email == null || email.isBlank()) return; // Line user ไม่มี email → ข้ามได้เลย
        Map<String, String> payload = new HashMap<>();
        payload.put("paymentStatus", status);
        messagingTemplate.convertAndSendToUser(email, "/queue/notifications", payload);
    }
    
    public void sendNotifyToUser(Notification request) {
        NotifyResponseDTO payload = NotifyResponseDTO.builder()
                .id(request.getId())
                .title(request.getTitle())
                .message(request.getMessage())
                .createdAt(request.getCreatedAt())
                .build();
        if(request.getSendTo().equals(SendTo.CUSTOMER)){
            messagingTemplate.convertAndSend("/topic/customer", payload);
        }else if (request.getSendTo().equals(SendTo.ALL)) {
            messagingTemplate.convertAndSend("/topic/all", payload);
        } else if (request.getSendTo().equals(SendTo.MODERATOR)) {
            messagingTemplate.convertAndSend("/topic/moderator", payload);
        }
    }
}
