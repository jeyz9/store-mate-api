package com.sm.jeyz9.storemateapi.controllers;

import com.sm.jeyz9.storemateapi.dto.OrderDTO;
import com.sm.jeyz9.storemateapi.dto.OrderDetailsDTO;
import com.sm.jeyz9.storemateapi.dto.OrderModDTO;
import com.sm.jeyz9.storemateapi.dto.OrderModDetailsDTO;
import com.sm.jeyz9.storemateapi.dto.OrderStatusRequestDTO;
import com.sm.jeyz9.storemateapi.dto.RefundDetailsDTO;
import com.sm.jeyz9.storemateapi.dto.RefundPaginationDTO;
import com.sm.jeyz9.storemateapi.dto.ShippingDTO;
import com.sm.jeyz9.storemateapi.dto.ShippingRequestDTO;
import com.sm.jeyz9.storemateapi.models.OrderStatusName;
import com.sm.jeyz9.storemateapi.services.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/v1")
public class OrderController {
    private final OrderService orderService;

    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }
    
    @GetMapping("/orders")
    public ResponseEntity<List<OrderDTO>> getUserOrders(@RequestParam("status")OrderStatusName status, Principal principal) {
        return new ResponseEntity<>(orderService.getUserOrders(status, principal.getName()), HttpStatus.OK);
    }
    
    @GetMapping("/orders/{orderNo}/status")
    public ResponseEntity<Map<String, String>> getOrderStatus(@PathVariable("orderNo") String orderNo) {
        return new ResponseEntity<>(Map.of("status", orderService.getOrderStatus(orderNo)), HttpStatus.OK);
    }
    
    @GetMapping("/orders/{orderNo}")
    public ResponseEntity<OrderDetailsDTO> getOrderDetails(@PathVariable("orderNo") String orderNo, Principal principal){
        return ResponseEntity.ok(orderService.getOrderDetails(principal.getName(), orderNo));
    }
    
    @PutMapping("/moderator/orders/{orderNo}/change-status")
    @Operation(description = """
        PENDING: รอดำเนินการ, รอชำระเงิน
        PROCESSING: ที่ต้องจัดส่ง
        RECEIVED: ที่ต้องได้รับ
        COMPLETED: คำสั่งซื้อสำเร็จ
    """)
    public ResponseEntity<String> changeOrderStatus(@PathVariable(name = "orderNo") String orderNo, @Valid @RequestBody OrderStatusRequestDTO request, Principal principal){
        return new ResponseEntity<> (orderService.changeOrderStatus(orderNo, request.getStatus(), principal.getName()), HttpStatus.OK);
    }
    
    @GetMapping("/moderator/orders")
    @Operation(description = """
        keyword: ค้นหาตาม ชื่อ, เบอร์โทร
        startDate: ค้นหาวันที่เริ่มต้น เช่น 2026-04-25 YYYY-MM-DD
        endDate: ค้นหาถึงวันที่ เช่น 2026-04-25 YYYY-MM-DD
        period: TODAY, WEEK, MONTH
    """)
    public ResponseEntity<Page<OrderModDTO>> getAllOrders(
            @RequestParam(name = "keyword", required = false) String keyword, 
            @RequestParam(name = "startDate", required = false) LocalDate startDate, 
            @RequestParam(name = "endDate", required = false) LocalDate endDate, 
            @RequestParam(name = "period", required = false) String period,
            @RequestParam(defaultValue = "0", required = false) Integer page,
            @RequestParam(defaultValue = "10", required = false) Integer size
    ) {
        return new ResponseEntity<>(orderService.getAllOrders(keyword, startDate, endDate, period, page, size), HttpStatus.OK);
    }
    
    @GetMapping("/moderator/orders/{orderNo}")
    public ResponseEntity<OrderModDetailsDTO> getOrderDetailsByModerator(@PathVariable("orderNo") String orderNo) {
        return new ResponseEntity<>(orderService.getOrderDetailsByModerator(orderNo), HttpStatus.OK);
    }
    
    @PostMapping("/moderator/orders/shipping-label")
    public ResponseEntity<List<ShippingDTO>> printShippingLabel(@RequestBody ShippingRequestDTO request) {
        return new ResponseEntity<>(orderService.printShippingLabel(request.getIds()), HttpStatus.OK);
    }
    
    @GetMapping("/moderator/orders/refund")
    public ResponseEntity<RefundPaginationDTO> getAllOrdersRefund(@RequestParam(required = false, defaultValue = "0") Integer page, @RequestParam(required = false, defaultValue = "6") Integer size) {
        return new ResponseEntity<>(orderService.getAllOrdersRefund(page, size), HttpStatus.OK);
    }
    
    @GetMapping("/moderator/orders/refund/{refundNo}")
    public ResponseEntity<RefundDetailsDTO> getOrderRefundDetails(@PathVariable("refundNo") String refundNo) {
        return new ResponseEntity<>(orderService.getOrderRefundDetails(refundNo), HttpStatus.OK);
    }
}
