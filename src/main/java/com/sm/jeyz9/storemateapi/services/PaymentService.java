package com.sm.jeyz9.storemateapi.services;

import com.sm.jeyz9.storemateapi.dto.CheckoutNowRequestDTO;
import com.sm.jeyz9.storemateapi.dto.CheckoutRequestDTO;
import com.sm.jeyz9.storemateapi.exceptions.WebException;
import com.sm.jeyz9.storemateapi.models.CartItem;
import com.sm.jeyz9.storemateapi.models.CheckoutTypeName;
import com.sm.jeyz9.storemateapi.models.Order;
import com.sm.jeyz9.storemateapi.models.OrderAddress;
import com.sm.jeyz9.storemateapi.models.OrderChannelName;
import com.sm.jeyz9.storemateapi.models.OrderItem;
import com.sm.jeyz9.storemateapi.models.OrderStatusName;
import com.sm.jeyz9.storemateapi.models.Product;
import com.sm.jeyz9.storemateapi.models.User;
import com.sm.jeyz9.storemateapi.models.UserAddress;
import com.sm.jeyz9.storemateapi.repository.CartItemRepository;
import com.sm.jeyz9.storemateapi.repository.OrderAddressRepository;
import com.sm.jeyz9.storemateapi.repository.OrderItemRepository;
import com.sm.jeyz9.storemateapi.repository.OrderRepository;
import com.sm.jeyz9.storemateapi.repository.ProductRepository;
import com.sm.jeyz9.storemateapi.repository.UserAddressRepository;
import com.sm.jeyz9.storemateapi.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PaymentService {

    private final UserRepository userRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserAddressRepository userAddressRepository;
    private final OrderAddressRepository orderAddressRepository;
    private final ProductRepository productRepository;

    @Value("${stripe.secret-key}")
    private String secretKey;
    
    @Value("${stripe.success-url}")
    private String successUrl;
    
    @Value("${stripe.cancel-url}")
    private String cancelUrl;

    @Autowired
    public PaymentService(UserRepository userRepository, CartItemRepository cartItemRepository, OrderRepository orderRepository, OrderItemRepository orderItemRepository, UserAddressRepository userAddressRepository, OrderAddressRepository orderAddressRepository, ProductRepository productRepository) {
        this.userRepository = userRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.userAddressRepository = userAddressRepository;
        this.orderAddressRepository = orderAddressRepository;
        this.productRepository = productRepository;
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
    public Map<String, String> checkoutIntent(String email, CheckoutRequestDTO request) {
        try {
            User user = userRepository.findUserByEmail(email).orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "User not found"));

            Order order = Order.builder()
                    .id(null)
                    .status(OrderStatusName.PENDING)
                    .user(user)
                    .createdAt(LocalDateTime.now())
                    .orderChannel(OrderChannelName.ONLINE)
                    .checkoutType(request.getCheckoutType())
                    .build();

            List<OrderItem> orderItems = request.getIds().stream().map(c -> {
                CartItem cartItem = cartItemRepository.findById(c).orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "Cart Item not found"));
                if (!cartItem.getCart().getUser().getId().equals(user.getId())) {
                    throw new WebException(HttpStatus.BAD_REQUEST, "Create order fail");
                }
                
                cartItemRepository.delete(cartItem);

                return OrderItem.builder()
                        .id(null)
                        .product(cartItem.getProduct())
                        .quantity(cartItem.getQuantity())
                        .order(order)
                        .build();
            }).toList();

            Map<String, String> res = new HashMap<>();

            if (request.getCheckoutType().equals(CheckoutTypeName.CARD) || request.getCheckoutType().equals(CheckoutTypeName.PROMPTPAY)) {
                Long total = (long) orderItems.stream().mapToDouble(i -> i.getProduct().getPrice() * i.getQuantity()).sum() * 100;
                PaymentIntent intent = handleStripeIntent(total);
                order.setStripePaymentIntent(intent.getId());
                order.setClientSecret(intent.getClientSecret());

                orderRepository.save(order);
                orderItemRepository.saveAll(orderItems);
                handleCreateOrderAddress(user, order);
                res.put("clientSecret", intent.getClientSecret());
                res.put("paymentIntentId", intent.getId());

                return res;
            }else if(request.getCheckoutType().equals(CheckoutTypeName.DESTINATION)) {
                order.setStatus(OrderStatusName.PROCESSING);
            }

            orderRepository.save(order);
            orderItemRepository.saveAll(orderItems);
            handleCreateOrderAddress(user, order);
            res.put("message", "create order success");
            return res;
        }catch (WebException e) {
            throw e;
        }catch (Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error: " + e.getMessage());
        }
    }
    
    @Transactional
    public Map<String, String> checkoutNow(String email, CheckoutNowRequestDTO request) {
        try {
            User user = userRepository.findUserByEmail(email).orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "User not found"));
            Product product = productRepository.findById(request.getId()).orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "Product not found"));
            if (!(product.getStock_quantity() > 0 && product.getProductStatus().getId().equals(1L) && product.getStock_quantity() >= request.getQuantity())) {
                throw new WebException(HttpStatus.BAD_REQUEST, "Product is unavailable");
            }

            Order order = Order.builder()
                    .id(null)
                    .status(OrderStatusName.PENDING)
                    .user(user)
                    .createdAt(LocalDateTime.now())
                    .orderChannel(OrderChannelName.ONLINE)
                    .checkoutType(request.getCheckoutType())
                    .build();

            OrderItem orderItem = OrderItem.builder()
                    .id(null)
                    .product(product)
                    .quantity(request.getQuantity())
                    .order(order)
                    .build();

            Map<String, String> res = new HashMap<>();

            if (request.getCheckoutType().equals(CheckoutTypeName.CARD) || request.getCheckoutType().equals(CheckoutTypeName.PROMPTPAY)) {
                Long total = (long) (orderItem.getProduct().getPrice() * orderItem.getQuantity()) * 100;
                PaymentIntent intent = handleStripeIntent(total);
                order.setStripePaymentIntent(intent.getId());
                order.setClientSecret(intent.getClientSecret());

                orderRepository.save(order);
                orderItemRepository.save(orderItem);
                handleCreateOrderAddress(user, order);
                res.put("clientSecret", intent.getClientSecret());
                res.put("paymentIntentId", intent.getId());

                return res;
            }else if(request.getCheckoutType().equals(CheckoutTypeName.DESTINATION)) {
                order.setStatus(OrderStatusName.PROCESSING);
            }

            orderRepository.save(order);
            orderItemRepository.save(orderItem);
            handleCreateOrderAddress(user, order);
            res.put("message", "create order success");
            return res;
        }catch (WebException e) {
            throw e;
        }catch (Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error: " + e.getMessage());
        }
    }
    
    public void handleStripeWebhook(Event event) throws EventDataObjectDeserializationException {
        switch (event.getType()) {
            case "payment_intent.succeeded":
                handlePaymentSucceeded(event);
                break;
            case "payment_intent.failed":
                handlePaymentFailed(event);
                break;
            default:
                throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "Unknown intent " + event.getType());
        }
    }

    private void handlePaymentSucceeded(Event event) throws EventDataObjectDeserializationException {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        PaymentIntent intent = null;
        if (deserializer.getObject().isPresent()) {
            intent = (PaymentIntent) deserializer.getObject().get();
        } else {
            intent = (PaymentIntent) deserializer.deserializeUnsafe();
        }

        markAsPaid(intent.getId());
    }

    private void handlePaymentFailed(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();

        if (deserializer.getObject().isPresent()) {
            PaymentIntent intent = (PaymentIntent) deserializer.getObject().get();

            String paymentIntentId = intent.getId();

            Optional<Order> optionalOrder = orderRepository.findByStripePaymentIntent(paymentIntentId);

            if (optionalOrder.isEmpty()) {
                System.out.println("Order not found: " + paymentIntentId);
                return;
            }

            Order order = optionalOrder.get();

            if (order.getStatus() == OrderStatusName.PROCESSING) {
                return;
            }

//            if (intent.getLastPaymentError() != null) {
//                order.setFailReason(intent.getLastPaymentError().getMessage());
//            }

            orderRepository.save(order);
        }
    }
    
    private void markAsPaid(String clientSecret) {
        Order order = orderRepository.findByStripePaymentIntent(clientSecret).orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "Intent not found"));
        order.setPaidAt(LocalDateTime.now());
        order.setStatus(OrderStatusName.PROCESSING);
        orderRepository.save(order);
    }
    
    private PaymentIntent handleStripeIntent(Long total) throws StripeException {
        Stripe.apiKey = secretKey;
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(total)
                .setCurrency("thb")
                .addPaymentMethodType("card")
                .addPaymentMethodType("promptpay")
                .build();
        
        return PaymentIntent.create(params);
    }
    
    private void handleCreateOrderAddress(User user, Order order) {
        UserAddress userAddress = userAddressRepository.findByUserIdAndIsDefaultTrue(user.getId()).orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "User address not found"));

        OrderAddress orderAddress = OrderAddress.builder()
                .id(null)
                .streetAddress(userAddress.getStreetAddress())
                .order(order)
                .zipcode(userAddress.getZipcode())
                .createdAt(LocalDateTime.now())
                .build();
        
        orderAddressRepository.save(orderAddress);
    }
}
