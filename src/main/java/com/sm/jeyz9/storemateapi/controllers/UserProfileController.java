package com.sm.jeyz9.storemateapi.controllers;

import com.sm.jeyz9.storemateapi.dto.UserProfileRequestDTO;
import com.sm.jeyz9.storemateapi.models.User;
import com.sm.jeyz9.storemateapi.services.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @Operation(summary = "ดูหน้าโปรไฟล์ผู้ใช้")
    @GetMapping("/overview")
    public ResponseEntity<UserProfileRequestDTO> getMyProfile(Principal principal) {
        return ResponseEntity.ok(userProfileService.getUserProfile(principal.getName()));
    }

  
}