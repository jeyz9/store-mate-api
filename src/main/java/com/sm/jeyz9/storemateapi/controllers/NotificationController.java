package com.sm.jeyz9.storemateapi.controllers;

import com.sm.jeyz9.storemateapi.dto.NotifyOwnerResponseDTO;
import com.sm.jeyz9.storemateapi.dto.NotifyRequestDTO;
import com.sm.jeyz9.storemateapi.dto.NotifyResponseDTO;
import com.sm.jeyz9.storemateapi.services.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public ResponseEntity<Page<NotifyOwnerResponseDTO>> getAllNotify(
            @RequestParam(defaultValue = "", required = false) String keyword, 
            @RequestParam(defaultValue = "0", required = false) Integer page,
            @RequestParam(defaultValue = "10", required = false) Integer size
    ) {
        return ResponseEntity.ok(notificationService.getAllNotify(keyword, page, size));
    }
    
    @DeleteMapping("/owner/notify/{notifyId}")
    public ResponseEntity<String> removeNotify(@PathVariable("notifyId") Long notifyId) {
        return ResponseEntity.ok(notificationService.removeNotify(notifyId));
    }
    
    @Operation(
            description = """
                ประเภทแจ้งเตือน (type)
                ALL: ทั้งหมด
                STORE: ร้านค้า
                ORDERED: ออร์เดอร์
                REFUNDED: คืนเงิน
            """
    )
    @GetMapping("/notify")
    public ResponseEntity<List<NotifyResponseDTO>> getNotifyUser(@RequestParam(required = false, defaultValue = "ALL") String type, Principal principal) {
        return ResponseEntity.ok(notificationService.getAllNotifyUser(principal.getName(), type));
    }
}
