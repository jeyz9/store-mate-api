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
        // ดึงโปรไฟล์ด้วย Email (ปกติ principal.getName() จะคืนค่า username/email)
        return ResponseEntity.ok(userProfileService.getUserProfile(principal.getName()));
    }
    @Operation(summary = "แก้ไขโปรไฟล์ผู้ใช้")
    @PutMapping("/overview")
    public ResponseEntity<User> updateMyProfile(@RequestBody UserProfileRequestDTO dto, Principal principal) {
        try {
            // แปลงค่าจาก Principal Name เป็น Long ID
            Long userId = Long.valueOf(principal.getName());
            return ResponseEntity.ok(userProfileService.updateProfile(userId, dto));
        } catch (NumberFormatException e) {
            // กรณีที่ principal.getName() ไม่ใช่ตัวเลข (เช่น เป็น email) จะเกิด error ตรงนี้
            throw new RuntimeException("รูปแบบ User ID ไม่ถูกต้อง");
        }
    }
}