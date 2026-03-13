package com.sm.jeyz9.storemateapi.services;

import com.sm.jeyz9.storemateapi.dto.CartItemRequestDTO;
import com.sm.jeyz9.storemateapi.models.CartItem;

import java.util.List;

public interface CartService {
    String addProductToCart(String email, CartItemRequestDTO request);
    List<CartItem> getCartItems(String email);
    String updateQuantity(String email, Long productId, int delta);
    String removeItem(String email, Long productId);
}
