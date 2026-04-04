package com.sm.jeyz9.storemateapi.controllers;

import com.sm.jeyz9.storemateapi.dto.OrderRequestDTO;
import com.sm.jeyz9.storemateapi.services.PaymentService;
import com.stripe.exception.StripeException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/v1")
public class PaymentController {
    private final PaymentService paymentService;

    @Autowired
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/orders/test/payments")
    public ResponseEntity<String> checkout() throws StripeException {
        return new ResponseEntity<>(paymentService.checkout(), HttpStatus.OK);
    }
    
    @PostMapping("/orders/payments/intent")
    public ResponseEntity<Map<String, String>> checkoutIntent(@RequestBody OrderRequestDTO request, Principal principal) throws StripeException {
        return new ResponseEntity<>(paymentService.checkoutIntent(principal.getName(), request.getIds()), HttpStatus.OK);
    }
    
    @PostMapping("/stripe/webhook")
    public ResponseEntity<String> handleStripeWebhook(HttpServletRequest request) throws Exception {
        return new ResponseEntity<>(paymentService.handleStripeWebhook(request), HttpStatus.OK);
    }
}
