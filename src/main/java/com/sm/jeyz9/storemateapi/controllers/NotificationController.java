package com.sm.jeyz9.storemateapi.controllers;

import com.sm.jeyz9.storemateapi.services.MessagingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class NotificationController {
    private final MessagingService messagingService;
    
    @Autowired
    public NotificationController(MessagingService messagingService) {
        this.messagingService = messagingService;
    }
    
    @PostMapping("/test/notify/send")
    public ResponseEntity<String> sendNotify(@RequestBody String role) {
        messagingService.sendNotifyToUser(role);
        return new ResponseEntity<>("Send success", HttpStatus.CREATED);
    }
}
