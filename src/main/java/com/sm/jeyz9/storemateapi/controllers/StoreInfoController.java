package com.sm.jeyz9.storemateapi.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sm.jeyz9.storemateapi.dto.StoreInfoDTO;
import com.sm.jeyz9.storemateapi.dto.StoreInfoRequestDTO;
import com.sm.jeyz9.storemateapi.services.StoreInfoService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/owner")
@RequiredArgsConstructor
public class StoreInfoController {

    private final StoreInfoService storeInfoService;

    @Operation(summary = "แสดงข้อมูลของร้านค้า")
    @GetMapping("/store")
    public ResponseEntity<StoreInfoDTO> getStoreDetails() {
        return ResponseEntity.ok(storeInfoService.getStoreDetails());
    }


    @Operation(summary = "แก้ไขข้อมูลของร้านค้า", description = """
                ใช้สำหรับอัปเดตข้อมูลร้านค้า พร้อมรองรับการอัปโหลดรูปภาพร้านค้า 1 ภาพ
                (ส่งเฉพาะฟิลด์ที่ต้องการเปลี่ยนแปลง ระบบจะคงค่าเดิมไว้หากฟิลด์นั้นเป็น null)
       
                ตัวอย่าง Request:
                {
                  "storeName": "ชื่อร้านใหม่",
                  "phone": "0812345678",
                  "streetAddress": "123 ถนนสุขุมวิท",
                  "email": "store@example.com"
                }
       
                หรือจะเปลี่ยนแค่บางฟิลด์ก็ได้
                {
                  "storeName": "ชื่อร้านใหม่"
                }
       
                เงื่อนไขการอัปเดต:
                - storeName: ชื่อร้านค้า
                - phone: เบอร์โทรศัพท์ร้านค้า
                - streetAddress: ที่อยู่ร้านค้า
                - email: อีเมลร้านค้า
                - image: รูปภาพร้านค้า
       """
    )


    @PutMapping(value = "/store/{storeId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StoreInfoDTO> updateStoreDetails(
            @PathVariable Long storeId,
            @RequestPart(value = "data", required = false) String requestJson,
            @RequestPart(value = "image", required = false) MultipartFile image,
            Principal principal) throws JsonProcessingException {

        ObjectMapper mapper = new ObjectMapper();
        StoreInfoRequestDTO request = (requestJson != null)
                ? mapper.readValue(requestJson, StoreInfoRequestDTO.class)
                : new StoreInfoRequestDTO();

        return ResponseEntity.ok(storeInfoService.updateStoreDetails(storeId, request, principal.getName(), image));
    }
}
