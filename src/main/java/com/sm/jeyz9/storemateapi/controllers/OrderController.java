package com.sm.jeyz9.storemateapi.controllers;

import com.sm.jeyz9.storemateapi.dto.OrderDTO;
import com.sm.jeyz9.storemateapi.models.OrderStatusName;
import com.sm.jeyz9.storemateapi.services.NotificationService;
import com.sm.jeyz9.storemateapi.services.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/v1/orders")
public class OrderController {
    private final OrderService orderService;
    private final NotificationService notificationService;

    @Autowired
    public OrderController(OrderService orderService, NotificationService notificationService) {
        this.orderService = orderService;
        this.notificationService = notificationService;
    }
    
    @GetMapping
    public ResponseEntity<List<OrderDTO>> getUserOrders(@RequestParam("status")OrderStatusName status, Principal principal) {
        return new ResponseEntity<>(orderService.getUserOrders(status, principal.getName()), HttpStatus.OK);
    }
    
    @GetMapping("/{orderNo}/status")
    public ResponseEntity<Map<String, String>> getOrderStatus(@PathVariable("orderNo") String orderNo) {
        return new ResponseEntity<>(Map.of("status", orderService.getOrderStatus(orderNo)), HttpStatus.OK);
    }

    @GetMapping("/test-ws")
    public void testWs() {
        notificationService.sendToUser("test2@gmail.com", "TEST_MESSAGE");
    }
}
