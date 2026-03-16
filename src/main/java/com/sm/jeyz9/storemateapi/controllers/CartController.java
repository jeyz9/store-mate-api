package com.sm.jeyz9.storemateapi.controllers;

import com.sm.jeyz9.storemateapi.dto.CartItemDTO;
import com.sm.jeyz9.storemateapi.dto.CartItemRequestDTO;
import com.sm.jeyz9.storemateapi.models.CartItem;
import com.sm.jeyz9.storemateapi.services.CartService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {
    private final CartService cartService;

    @Autowired
    public CartController(CartService cartService) {
        this.cartService = cartService;
    }
    
    @GetMapping("/items")
    public ResponseEntity<List<CartItemDTO>> getCart(Principal principal) {
        return ResponseEntity.ok(cartService.getCartItems(principal.getName()));
    }
    
    @PostMapping("/items")
    public ResponseEntity<String> addProductToCart(@Valid @RequestBody CartItemRequestDTO request, Principal principal) {
        return new ResponseEntity<>(cartService.addProductToCart(principal.getName(), request), HttpStatus.CREATED);
    }

    @PatchMapping("/items/{productId}/increment")
    public ResponseEntity<String> increment(@PathVariable Long productId, Principal principal) {
        return ResponseEntity.ok(cartService.updateQuantity(principal.getName(), productId, 1));
    }

    @PatchMapping("/items/{productId}/decrement")
    public ResponseEntity<String> decrement(@PathVariable Long productId, Principal principal) {
        return ResponseEntity.ok(cartService.updateQuantity(principal.getName(), productId, -1));
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<String> remove(@PathVariable Long productId, Principal principal) {
        return ResponseEntity.ok(cartService.removeItem(principal.getName(), productId));
    }


}
