package com.sm.jeyz9.storemateapi.services;

import com.sm.jeyz9.storemateapi.dto.CartItemDTO;
import com.sm.jeyz9.storemateapi.dto.CartItemRequestDTO;

import java.util.List;

public interface CartService {
    String addProductToCart(String email, CartItemRequestDTO request);
    List<CartItemDTO> getCartItems(String email);
    String updateQuantity(String email, Long productId, int delta);
    String removeItem(String email, Long productId);
}
