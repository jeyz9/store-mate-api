package com.sm.jeyz9.storemateapi.services;

import com.sm.jeyz9.storemateapi.dto.OrderDTO;
import com.sm.jeyz9.storemateapi.dto.OrderItemDTO;
import com.sm.jeyz9.storemateapi.exceptions.WebException;
import com.sm.jeyz9.storemateapi.models.CartItem;
import com.sm.jeyz9.storemateapi.models.Order;
import com.sm.jeyz9.storemateapi.models.OrderChannelName;
import com.sm.jeyz9.storemateapi.models.OrderItem;
import com.sm.jeyz9.storemateapi.models.OrderStatusName;
import com.sm.jeyz9.storemateapi.models.ProductImage;
import com.sm.jeyz9.storemateapi.models.User;
import com.sm.jeyz9.storemateapi.repository.CartItemRepository;
import com.sm.jeyz9.storemateapi.repository.OrderItemRepository;
import com.sm.jeyz9.storemateapi.repository.OrderRepository;
import com.sm.jeyz9.storemateapi.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PaymentService {

    private final UserRepository userRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    @Value("${stripe.secret-key}")
    private String secretKey;
    
    @Value("${stripe.success-url}")
    private String successUrl;
    
    @Value("${stripe.cancel-url}")
    private String cancelUrl;

    public PaymentService(UserRepository userRepository, CartItemRepository cartItemRepository, OrderRepository orderRepository, OrderItemRepository orderItemRepository) {
        this.userRepository = userRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    public String checkout() throws StripeException {
        Stripe.apiKey = secretKey;
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.PROMPTPAY)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("thb")
                                                .setUnitAmount(10000L)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("Test Product")
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .build();
        
        return Session.create(params).getUrl();   
    }
    
    @Transactional
    public Map<String, String> checkoutIntent(String email, List<Long> cartItemIds) throws StripeException {
        User user = userRepository.findUserByEmail(email).orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "User not found"));
        
        Order order = Order.builder()
                .id(null)
                .status(OrderStatusName.PENDING)
                .user(user)
                .createdAt(LocalDateTime.now())
                .orderChannel(OrderChannelName.ONLINE)
                .build();
        
        List<OrderItem> orderItems = cartItemIds.stream().map(c -> {
            CartItem cartItem = cartItemRepository.findById(c).orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "Cart Item not found"));
            if (!cartItem.getCart().getUser().getId().equals(user.getId())) {
                throw new WebException(HttpStatus.BAD_REQUEST, "Create order fail");
            }
            
            cartItemRepository.delete(cartItem);

            OrderItem orderItem = OrderItem.builder()
                    .id(null)
                    .product(cartItem.getProduct())
                    .quantity(cartItem.getQuantity())
                    .order(order)
                    .build();
            
            return orderItemRepository.save(orderItem);
        }).toList();

        Stripe.apiKey = secretKey;
        Long total = (long) orderItems.stream().mapToDouble(i -> i.getProduct().getPrice() * i.getQuantity()).sum() * 100;
        
        System.out.println("TOTAL: " + total);

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(total)
                .setCurrency("thb")
                .addPaymentMethodType("card")
                .addPaymentMethodType("promptpay")
                .build();

        PaymentIntent intent = PaymentIntent.create(params);

        order.setStripePaymentIntent(intent.getId());
        order.setClientSecret(intent.getClientSecret());
        orderRepository.save(order);

        Map<String, String> res = new HashMap<>();
        res.put("clientSecret", intent.getClientSecret());
        res.put("paymentIntentId", intent.getId());
        return res;
    }
}
