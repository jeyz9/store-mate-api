package com.sm.jeyz9.storemateapi.controllers;

import com.sm.jeyz9.storemateapi.dto.PaginationDTO;
import com.sm.jeyz9.storemateapi.dto.UserManagementDTO;
import com.sm.jeyz9.storemateapi.dto.UserRoleRequestDTO;
import com.sm.jeyz9.storemateapi.services.UserManagementService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/owner")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserManagementService userManagementService;

    @Operation(
            summary = "แสดงผู้ใช้ทั้งหมด",
            description = """
                ดึงรายชื่อผู้ใช้งานทั้งหมดแบบแบ่งหน้า (Pagination)
                
                ตัวอย่าง URL : /api/v1/owner/users?page=0&size=5
                
                - page: หน้าที่ต้องการ (เริ่มจาก 0) เช่น หน้าแรก = 0, หน้าที่สอง = 1
                - size: จำนวนรายการต่อหน้า (default = 5) 
                    เปลี่ยนจำนวนที่จะให้แสดงได้ตามที่เราพิม
                
                ตัวอย่าง:
                - หน้าแรก 3 รายการ  → ?page=0&size=3
                - หน้าที่สอง 3 รายการ → ?page=1&size=3
                - หน้าที่สาม 10 รายการ → ?page=2&size=10
                
                Response จะมี:
                - data: รายชื่อผู้ใช้ในหน้านั้น
                - page: หน้าปัจจุบัน
                - size: จำนวนรายการต่อหน้า
                - total: จำนวนผู้ใช้ทั้งหมดในระบบ
                """
    )
    @GetMapping("/users")
    public ResponseEntity<PaginationDTO<UserManagementDTO>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            Principal principal) {
        return ResponseEntity.ok(userManagementService.getAllUsers(page, size, principal.getName()));
    }

    @Operation(summary = "เปลี่ยนบทบาทผู้ใช้งาน",
            description = """
                role ของผู้ใช้มี 3 ประเภท
                ADMIN
                MODERATOR 
                USER
    """
    )
    @PutMapping("/users/{userId}/roles")
    public ResponseEntity<UserManagementDTO> updateUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody UserRoleRequestDTO request,
            Principal principal) {
        return ResponseEntity.ok(userManagementService.updateUserRole(userId, request, principal.getName()));
    }

    @Operation(summary = "ระงับบัญชีผู้ใช้")
    @PutMapping("/users/{userId}/suspend")
    public ResponseEntity<UserManagementDTO> suspendUser(
            @PathVariable Long userId,
            Principal principal) {
        return ResponseEntity.ok(userManagementService.suspendUser(userId, principal.getName()));
    }

    @Operation(summary = "เปิดใช้งานบัญชีผู้ใช้")
    @PutMapping("/users/{userId}/activate")
    public ResponseEntity<UserManagementDTO> activateUser(
            @PathVariable Long userId,
            Principal principal) {
        return ResponseEntity.ok(userManagementService.activateUser(userId, principal.getName()));
    }
}