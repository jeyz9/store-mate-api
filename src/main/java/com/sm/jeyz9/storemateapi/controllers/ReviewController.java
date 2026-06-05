package com.sm.jeyz9.storemateapi.controllers;

import com.sm.jeyz9.storemateapi.dto.PaginationDTO;
import com.sm.jeyz9.storemateapi.dto.ReviewDTO;
import com.sm.jeyz9.storemateapi.dto.ReviewRequestDTO;
import com.sm.jeyz9.storemateapi.services.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1")
public class ReviewController {

    private final ReviewService reviewService;

    @Autowired
    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Operation(
            summary = "แสดงผู้ใช้ทั้งหมด",
            description = """
                ดึงรายชื่อผู้ใช้งานทั้งหมดแบบแบ่งหน้า (Pagination)
                
                ตัวอย่าง URL : /api/v1/reviews/{productId}?page=0&size=5
                
                - page: หน้าที่ต้องการ (เริ่มจาก 0) เช่น หน้าแรก = 0, หน้าที่สอง = 1
                - size: จำนวนรายการต่อหน้า (default = 5)
                    เปลี่ยนจำนวนที่จะให้แสดงได้ตามที่เราพิม
                
                ตัวอย่าง:
                - หน้าแรก 3 รายการ  → ?page=0&size=3
                - หน้าที่สอง 3 รายการ → ?page=1&size=3
                - หน้าที่สาม 10 รายการ → ?page=2&size=10
                
                Response จะมี:
                - data: review ใน productId นี้
                - page: หน้าปัจจุบัน
                - size: จำนวนรายการต่อหน้า
                """
    )
    @GetMapping("/reviews/{productId}")
    public ResponseEntity<PaginationDTO<ReviewDTO>> getReviewsByProductId(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        return ResponseEntity.ok(reviewService.getReviewsByProductId(productId, page, size));
    }

    @PostMapping("/reviews/{orderItemId}")
    public ResponseEntity<String> addReview(
            @PathVariable Long orderItemId,
            @Valid @RequestBody ReviewRequestDTO request,
            Principal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.addReview(orderItemId, principal.getName(), request));
    }

    @Operation(summary = "แก้ไขรีวิวสินค้า")
    @PutMapping("/reviews/{reviewId}")
    public ResponseEntity<String> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewRequestDTO request,
            Principal principal
    ) {
        return ResponseEntity.ok(reviewService.updateReview(reviewId, principal.getName(), request));
    }

    @Operation(summary = "ลบรีวิวสินค้า")
    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<String> deleteReview(
            @PathVariable Long reviewId,
            Principal principal
    ) {
        return ResponseEntity.ok(reviewService.deleteReview(reviewId, principal.getName()));
    }
}