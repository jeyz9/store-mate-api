package com.sm.jeyz9.storemateapi.services.impl;

import com.sm.jeyz9.storemateapi.dto.CartItemDTO;
import com.sm.jeyz9.storemateapi.dto.CartItemRequestDTO;
import com.sm.jeyz9.storemateapi.exceptions.WebException;
import com.sm.jeyz9.storemateapi.models.Cart;
import com.sm.jeyz9.storemateapi.models.CartItem;
import com.sm.jeyz9.storemateapi.models.CartStatusName;
import com.sm.jeyz9.storemateapi.models.Product;
import com.sm.jeyz9.storemateapi.models.User;
import com.sm.jeyz9.storemateapi.repository.CartItemRepository;
import com.sm.jeyz9.storemateapi.repository.CartRepository;
import com.sm.jeyz9.storemateapi.repository.ProductRepository;
import com.sm.jeyz9.storemateapi.repository.UserRepository;
import com.sm.jeyz9.storemateapi.services.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CartServiceImpl implements CartService {
    private final CartItemRepository cartItemRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Autowired
    public CartServiceImpl(CartItemRepository cartItemRepository, CartRepository cartRepository, UserRepository userRepository, ProductRepository productRepository) {
        this.cartItemRepository = cartItemRepository;
        this.cartRepository = cartRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public String addProductToCart(String email, CartItemRequestDTO request) {
        try {
            User user = userRepository.findUserByEmail(email).orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "User not found."));
            Product product = productRepository.findById(request.getProductId()).orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "Product not found."));


            if(product.getStock_quantity() < request.getQuantity()) throw new WebException(HttpStatus.BAD_REQUEST, "There is insufficient stock.");

            Cart cart = cartRepository.findCartByStatus(CartStatusName.ACTIVE).orElse(null);
            if(cart == null) {
                Cart newCart = Cart.builder()
                        .id(null)
                        .user(user)
                        .status(CartStatusName.ACTIVE)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                cart = cartRepository.save(newCart);
            }


            CartItem cartItem = cartItemRepository.findCartItemByIdAndCartId(cart.getId(), request.getProductId()).orElse(null);

            if(cartItem != null) {
                cartItem.setQuantity(cartItem.getQuantity() + request.getQuantity());
                cartItem.setUpdatedAt(LocalDateTime.now());
            } else {
                cartItem = CartItem.builder()
                        .id(null)
                        .cart(cart)
                        .product(product)
                        .createdAt(LocalDateTime.now())
                        .quantity(request.getQuantity())
                        .updatedAt(LocalDateTime.now())
                        .build();
            }

            cartItemRepository.save(cartItem);


            return "Add product to cart success";
        } catch (WebException e) {
            throw e;
        } catch(Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CartItemDTO> getCartItems(String email) {
        try {
            User user = userRepository.findUserByEmail(email)
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "User not found"));
            
            Cart cart = cartRepository.findCartByStatus(CartStatusName.ACTIVE)
                    .filter(c -> c.getUser().getId().equals(user.getId()))
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "No active cart found for this user"));
         
            return cartItemRepository.findAll().stream()
                    .filter(item -> item.getCart().getId().equals(cart.getId()))
                    .map(item -> {
                        Product p = item.getProduct();
                        return CartItemDTO.builder()
                                .productId(p.getId())
                                .productName(p.getName())
                                .imageUrl(p.getProductImage().isEmpty() ? null : p.getProductImage().get(0).getImageUrl())
                                .price(p.getPrice())
                                .quantity(item.getQuantity())
                                .subTotal(p.getPrice() * item.getQuantity())
                                .stockQuantity(p.getStock_quantity())
                                .productStatus(p.getProductStatus().getStatus())
                                .build();
                    })
                    .toList();
        } catch (WebException e) {
            throw e;
        } catch(Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public String updateQuantity(String email, Long productId, int delta) {
        try {
            Cart cart = cartRepository.findCartByStatus(CartStatusName.ACTIVE)
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "Cart not found"));

            CartItem cartItem = cartItemRepository.findCartItemByIdAndCartId(cart.getId(), productId)
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "Product not in cart"));

            int newQuantity = cartItem.getQuantity() + delta;

            if (newQuantity <= 0) {
                return removeItem(email, productId);
            }

            if (delta > 0 && cartItem.getProduct().getStock_quantity() < 1) {
                throw new WebException(HttpStatus.BAD_REQUEST, "Insufficient stock.");
            }

            cartItem.setQuantity(newQuantity);
            cartItem.setUpdatedAt(LocalDateTime.now());
            cartItemRepository.save(cartItem);


            return "Updated successfully";
        } catch (WebException e) {
            throw e;
        } catch(Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public String removeItem(String email, Long productId) {
        try {
            Cart cart = cartRepository.findCartByStatus(CartStatusName.ACTIVE)
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "Cart not found"));

            CartItem cartItem = cartItemRepository.findCartItemByIdAndCartId(cart.getId(), productId)
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "Product not in cart"));




            cartItemRepository.delete(cartItem);
            return "Removed successfully";
        } catch (WebException e) {
            throw e;
        } catch(Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error " + e.getMessage());
        }
    }
}

