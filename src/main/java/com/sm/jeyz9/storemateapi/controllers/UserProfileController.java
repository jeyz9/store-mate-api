package com.sm.jeyz9.storemateapi.controllers;

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
    public ResponseEntity<UserAddress> addUserAddress(
            @RequestBody UserAddressRequestDTO dto,
            Principal principal) {
        UserAddress newAddress = userProfileService.addUserAddress(principal.getName(), dto);
        return new ResponseEntity<>(newAddress, HttpStatus.CREATED);
    }
  
}