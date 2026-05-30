package com.sm.jeyz9.storemateapi.controllers;

import com.sm.jeyz9.storemateapi.dto.NotifyOwnerResponseDTO;
import com.sm.jeyz9.storemateapi.dto.NotifyRequestDTO;
import com.sm.jeyz9.storemateapi.dto.NotifyResponseDTO;
import com.sm.jeyz9.storemateapi.services.NotificationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class NotificationController {
    private final NotificationService notificationService;

    @Autowired
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    
    @PostMapping("/owner/notify/send")
    public ResponseEntity<String> sendNotify(@RequestBody @Valid NotifyRequestDTO request, Principal principal) {
        return new ResponseEntity<>(notificationService.sendNotification(principal.getName(), request), HttpStatus.CREATED);
    }
    
    @GetMapping("/owner/notify")
    public ResponseEntity<List<NotifyOwnerResponseDTO>> getAllNotify() {
        return ResponseEntity.ok(notificationService.getAllNotify());
    }
    
    @DeleteMapping("/owner/notify/{notifyId}")
    public ResponseEntity<String> removeNotify(@PathVariable("notifyId") Long notifyId) {
        return ResponseEntity.ok(notificationService.removeNotify(notifyId));
    }
    
    @GetMapping("/notify")
    public ResponseEntity<List<NotifyResponseDTO>> getNotifyUser(Principal principal) {
        return ResponseEntity.ok(notificationService.getAllNotifyUser(principal.getName()));
    }
}
