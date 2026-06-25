package com.sm.jeyz9.storemateapi.services;

import com.sm.jeyz9.storemateapi.dto.NotificationDTO;
import com.sm.jeyz9.storemateapi.dto.NotifyRequestDTO;
import com.sm.jeyz9.storemateapi.dto.NotifyResponseDTO;
import com.sm.jeyz9.storemateapi.exceptions.WebException;
import com.sm.jeyz9.storemateapi.models.Notification;
import com.sm.jeyz9.storemateapi.models.NotificationRecipient;
import com.sm.jeyz9.storemateapi.models.SendTo;
import com.sm.jeyz9.storemateapi.models.User;
import com.sm.jeyz9.storemateapi.repository.NotificationRecipientRepository;
import com.sm.jeyz9.storemateapi.repository.NotificationRepository;
import com.sm.jeyz9.storemateapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class MessagingService {
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationRecipientRepository notificationRecipientRepository;

    @Autowired
    public MessagingService(SimpMessagingTemplate messagingTemplate, NotificationRepository notificationRepository, UserRepository userRepository, NotificationRecipientRepository notificationRecipientRepository) {
        this.messagingTemplate = messagingTemplate;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.notificationRecipientRepository = notificationRecipientRepository;
    }

    public void sendToUser(String email, String status) {
        if (email == null || email.isBlank()) return; // Line user ไม่มี email → ข้ามได้เลย
        Map<String, String> payload = new HashMap<>();
        payload.put("paymentStatus", status);
        messagingTemplate.convertAndSendToUser(email, "/queue/notifications", payload);
    }
    
    public void sendNotifyToUser(String email, NotificationDTO request) {
        if(email == null || email.isBlank()) return;
        User user = userRepository.findUserByEmail(email).orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "User not found"));
        Notification payload = Notification.builder()
                .id(null)
                .title(request.getTitle())
                .message(request.getMessage())
                .createdAt(LocalDateTime.now())
                .notifyType(request.getNotifyType())
                .build();
        notificationRepository.save(payload);
        NotificationRecipient recipient = NotificationRecipient.builder()
                .id(null)
                .user(user)
                .isRead(false)
                .notify(payload)
                .build();
        notificationRecipientRepository.save(recipient);
        messagingTemplate.convertAndSendToUser(email, "/queue/notify", payload);
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
