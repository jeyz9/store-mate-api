package com.sm.jeyz9.storemateapi.services;

import com.sm.jeyz9.storemateapi.models.RoleName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.parameters.P;
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
        Map<String, String> payload = new HashMap<>();
        payload.put("paymentStatus", status);
        messagingTemplate.convertAndSendToUser(email, "/queue/notifications", payload);
    }
    
    public void sendNotifyToUser(String role) {
        Map<String, String> payload = new HashMap<>();
        payload.put("TEST", "MESSAGE");
        System.out.println("SENDING MESSAGE");
        System.out.printf("input: %s : pk: %s", RoleName.valueOf(role), RoleName.USER);
        if(RoleName.valueOf(role).equals(RoleName.USER)){
            messagingTemplate.convertAndSend("/topic/customer", payload);
        }else if (RoleName.valueOf(role).equals(RoleName.ADMIN)) {
            messagingTemplate.convertAndSend("/topic/admin", payload);
        } else if (RoleName.valueOf(role).equals(RoleName.MODERATOR)) {
            messagingTemplate.convertAndSend("/topic/moderator", payload);
        }
    }
}
