package com.sm.jeyz9.storemateapi.controllers;

import com.sm.jeyz9.storemateapi.dto.OrderDTO;
import com.sm.jeyz9.storemateapi.models.OrderStatusName;
import com.sm.jeyz9.storemateapi.services.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/orders")
public class OrderController {
    private final OrderService orderService;
    
    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }
    
    @GetMapping
    public ResponseEntity<List<OrderDTO>> getUserOrders(@RequestParam("status")OrderStatusName status, Principal principal) {
        return new ResponseEntity<>(orderService.getUserOrders(status, principal.getName()), HttpStatus.OK);
    }
}
