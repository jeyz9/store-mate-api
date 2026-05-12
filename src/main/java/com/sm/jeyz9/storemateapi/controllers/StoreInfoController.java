package com.sm.jeyz9.storemateapi.controllers;

import com.sm.jeyz9.storemateapi.dto.StoreInfoDTO;
import com.sm.jeyz9.storemateapi.dto.StoreInfoRequestDTO;
import com.sm.jeyz9.storemateapi.models.User;
import com.sm.jeyz9.storemateapi.services.StoreInfoService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/owner")
@RequiredArgsConstructor
public class StoreInfoController {

    private final StoreInfoService storeInfoService;

    @Operation(summary = "แสดงข้อมูลของร้านค้า")
    @GetMapping("/store")
    public ResponseEntity<StoreInfoDTO> getStoreDetails(Principal principal) {
        return ResponseEntity.ok(storeInfoService.getStoreDetails(principal.getName()));
    }


    @Operation(summary = "แก้ไขข้อมูลของร้านค้า")
    @PutMapping("/store/{storeId}")
    public ResponseEntity<StoreInfoDTO> updateStoreDetails(
            @PathVariable Long storeId,
            @Valid @RequestBody StoreInfoRequestDTO request,
            Principal principal) {
        return ResponseEntity.ok(storeInfoService.updateStoreDetails(storeId, request, principal.getName()));
    }
}
