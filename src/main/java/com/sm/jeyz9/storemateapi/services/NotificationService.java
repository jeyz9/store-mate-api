package com.sm.jeyz9.storemateapi.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class NotificationService {
    private final SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    public NotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendToUser(String email, String status) {
        Map<String, String> payload = new HashMap<>();
        payload.put("paymentStatus", status);
        messagingTemplate.convertAndSendToUser(email, "/queue/notifications", payload);
    }
}
