package com.sm.jeyz9.storemateapi.services.impl;

import com.sm.jeyz9.storemateapi.dto.CartItemDTO;
import com.sm.jeyz9.storemateapi.dto.CartItemRequestDTO;
import com.sm.jeyz9.storemateapi.exceptions.WebException;
import com.sm.jeyz9.storemateapi.models.Cart;
import com.sm.jeyz9.storemateapi.models.CartItem;
import com.sm.jeyz9.storemateapi.models.CartStatusName;
import com.sm.jeyz9.storemateapi.models.Product;
import com.sm.jeyz9.storemateapi.models.ProductStatusName;
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

    // ─── Public API (email-based, used by the REST controller) ────────────────

    @Override
    @Transactional
    public String addProductToCart(String email, CartItemRequestDTO request) {
        return addProductToCartForUser(resolveUserByEmail(email), request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CartItemDTO> getCartItems(String email) {
        return getCartItemsForUser(resolveUserByEmail(email));
    }

    @Override
    @Transactional
    public String updateQuantity(String email, Long productId, int delta) {
        return updateQuantityForUser(resolveUserByEmail(email), productId, delta);
    }

    @Override
    @Transactional
    public String removeItem(String email, Long productId) {
        return removeItemForUser(resolveUserByEmail(email), productId);
    }

    // ─── Public API (userId-based, used by the LINE chatbot) ──────────────────

    @Override
    @Transactional
    public String addProductToCartByUserId(Long userId, CartItemRequestDTO request) {
        return addProductToCartForUser(resolveUserById(userId), request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CartItemDTO> getCartItemsByUserId(Long userId) {
        return getCartItemsForUser(resolveUserById(userId));
    }

    @Override
    @Transactional
    public String updateQuantityByUserId(Long userId, Long productId, int delta) {
        return updateQuantityForUser(resolveUserById(userId), productId, delta);
    }

    @Override
    @Transactional
    public String removeItemByUserId(Long userId, Long productId) {
        return removeItemForUser(resolveUserById(userId), productId);
    }

    // ─── Shared implementation ─────────────────────────────────────────────────
    // Both entry points (email / userId) resolve to the same User and share this
    // logic instead of duplicating it, so the two call paths can never drift
    // out of sync again.

    private String addProductToCartForUser(User user, CartItemRequestDTO request) {
        try {
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "Product not found."));

            if (product.getProductStatus() == null ||
                    !product.getProductStatus().getStatus().equals(ProductStatusName.ACTIVE.name())) {
                throw new WebException(HttpStatus.BAD_REQUEST, "สินค้านี้ไม่สามารถเพิ่มในตะกร้าได้");
            }

            Cart cart = findOrCreateActiveCart(user);

            CartItem cartItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId()).orElse(null);
            int currentQuantityInCart = (cartItem != null) ? cartItem.getQuantity() : 0;
            int totalNewQuantity = currentQuantityInCart + request.getQuantity();

            if (product.getStock_quantity() < totalNewQuantity) {
                throw new WebException(HttpStatus.BAD_REQUEST, "สินค้าในสต็อกไม่เพียงพอ (คุณมีในตะกร้าแล้ว " + currentQuantityInCart + " ชิ้น)");
            }

            if (cartItem != null) {
                cartItem.setQuantity(totalNewQuantity);
            } else {
                cartItem = CartItem.builder()
                        .cart(cart)
                        .product(product)
                        .quantity(request.getQuantity())
                        .build();
            }
            cartItemRepository.save(cartItem);
            return "Add product to cart success";

        } catch (WebException e) {
            throw e;
        } catch (Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error " + e.getMessage());
        }
    }

    private List<CartItemDTO> getCartItemsForUser(User user) {
        try {
            Cart cart = cartRepository.findCartByStatusAndUserId(CartStatusName.ACTIVE, user.getId())
                    .orElse(null);

            if (cart == null) {
                return List.of();
            }

            return cartItemRepository.findByCartId(cart.getId()).stream()
                    .map(this::toCartItemDTO)
                    .toList();
        } catch (WebException e) {
            throw e;
        } catch (Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error " + e.getMessage());
        }
    }

    private String updateQuantityForUser(User user, Long productId, int delta) {
        try {
            Cart cart = cartRepository.findCartByStatusAndUserId(CartStatusName.ACTIVE, user.getId())
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "Cart not found"));

            CartItem cartItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "Product not in cart"));

            int newQuantity = cartItem.getQuantity() + delta;

            if (newQuantity <= 0) {
                return removeItemForUser(user, productId);
            }

            if (delta > 0 && cartItem.getProduct().getStock_quantity() < newQuantity) {
                throw new WebException(HttpStatus.BAD_REQUEST, "สินค้าในสต็อกไม่เพียงพอ");
            }

            cartItem.setQuantity(newQuantity);
            cartItemRepository.save(cartItem);

            return "Updated successfully";
        } catch (WebException e) {
            throw e;
        } catch (Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error " + e.getMessage());
        }
    }

    private String removeItemForUser(User user, Long productId) {
        try {
            Cart cart = cartRepository.findCartByStatusAndUserId(CartStatusName.ACTIVE, user.getId())
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "Cart not found"));

            CartItem cartItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "Product not in cart"));

            cartItemRepository.delete(cartItem);
            return "Removed successfully";
        } catch (WebException e) {
            throw e;
        } catch (Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error " + e.getMessage());
        }
    }

    // ─── Small helpers ──────────────────────────────────────────────────────────

    private User resolveUserByEmail(String email) {
        try {
            return userRepository.findUserByEmail(email)
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "User not found."));
        } catch (WebException e) {
            throw e;
        } catch (Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error " + e.getMessage());
        }
    }

    private User resolveUserById(Long userId) {
        try {
            return userRepository.findById(userId)
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "User not found."));
        } catch (WebException e) {
            throw e;
        } catch (Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error " + e.getMessage());
        }
    }

    private Cart findOrCreateActiveCart(User user) {
        return cartRepository.findCartByStatusAndUserId(CartStatusName.ACTIVE, user.getId())
                .orElseGet(() -> cartRepository.save(
                        Cart.builder()
                                .user(user)
                                .status(CartStatusName.ACTIVE)
                                .build()));
    }

    private CartItemDTO toCartItemDTO(CartItem item) {
        Product p = item.getProduct();
        return CartItemDTO.builder()
                .cartItemId(item.getId())
                .productId(p.getId())
                .productName(p.getName())
                .imageUrl(p.getProductImage().isEmpty() ? null : p.getProductImage().get(0).getImageUrl())
                .price(p.getPrice())
                .quantity(item.getQuantity())
                .subTotal(p.getPrice() * item.getQuantity())
                .stockQuantity(p.getStock_quantity())
                .productStatus(p.getProductStatus() != null ? p.getProductStatus().getStatus() : null)
                .build();
    }
}
