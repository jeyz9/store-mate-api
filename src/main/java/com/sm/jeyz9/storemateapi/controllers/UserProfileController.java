package com.sm.jeyz9.storemateapi.controllers;

import com.sm.jeyz9.storemateapi.dto.UserAddressDTO;
import com.sm.jeyz9.storemateapi.dto.UserAddressRequestDTO;
import com.sm.jeyz9.storemateapi.dto.UserProfileRequestDTO;
import com.sm.jeyz9.storemateapi.models.User;
import com.sm.jeyz9.storemateapi.models.UserAddress;
import com.sm.jeyz9.storemateapi.services.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.Map;

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

    @Operation(summary = "แก้ไขโปรไฟล์ผู้ใช้")
    @PutMapping(value = "/overview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<User> updateMyProfile(
            @RequestPart(value = "data", required = false) UserProfileRequestDTO dto,
            @RequestPart(value = "image", required = false) MultipartFile image,
            Principal principal) {

        User updatedUser = userProfileService.updateProfile(principal.getName(), dto, image);
        return ResponseEntity.ok(updatedUser);
    }

    @Operation(summary = "เพิ่มที่อยู่ใหม่")
    @PostMapping("/addresses")
    public ResponseEntity<UserAddressDTO> addUserAddress(
            @RequestBody UserAddressRequestDTO dto,
            Principal principal) {
        UserAddressDTO newAddress = userProfileService.addUserAddress(principal.getName(), dto);
        return new ResponseEntity<>(newAddress, HttpStatus.CREATED);
    }

    @Operation(summary = "ดึงรายการที่อยู่ทั้งหมดของผู้ใช้")
    @GetMapping("/addresses")
    public ResponseEntity<List<UserAddressDTO>> getMyAddresses(Principal principal) {
        return ResponseEntity.ok(userProfileService.getUserAddresses(principal.getName()));
    }

    @Operation(summary = "ดึงข้อมูลที่อยู่ตาม ID")
    @GetMapping("/addresses/{id}")
    public ResponseEntity<UserAddressDTO> getAddressById(
            @PathVariable Long id,
            Principal principal) {
        return ResponseEntity.ok(userProfileService.getUserAddressById(id, principal.getName()));
    }
    

    @Operation(summary = "ลบที่อยู่ผู้ใช้งาน")
    @DeleteMapping("/addresses/{id}")
    public ResponseEntity<Map<String, String>> deleteAddress(
            @PathVariable Long id,
            Principal principal) {
        userProfileService.deleteUserAddress(id, principal.getName());
        return ResponseEntity.ok(Map.of("message", "ลบที่อยู่สำเร็จ"));
    }

    @Operation(summary = "แก้ไขที่อยู่ผู้ใช้งาน")
    @PutMapping("/addresses/{id}")
    public ResponseEntity<UserAddressDTO> updateAddress(
            @PathVariable Long id,
            @RequestBody UserAddressRequestDTO dto,
            Principal principal) {

        UserAddressDTO updated = userProfileService.updateUserAddress(id, dto, principal.getName());
        return ResponseEntity.ok(updated);
    }
    
    @Operation(summary = "ตั้งค่าที่อยู่เป็นค่าเริ่มต้น")
    @PatchMapping("/addresses/{id}")
    public ResponseEntity<UserAddressDTO> setDefaultAddress(
            @PathVariable Long id,
            Principal principal) {
        UserAddressDTO updated = userProfileService.setDefaultAddress(id, principal.getName());
        return ResponseEntity.ok(updated);
    }
}