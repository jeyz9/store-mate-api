package com.sm.jeyz9.storemateapi.services;

import com.sm.jeyz9.storemateapi.dto.CheckoutNowRequestDTO;
import com.sm.jeyz9.storemateapi.dto.CheckoutRequestDTO;
import com.sm.jeyz9.storemateapi.dto.NotificationDTO;
import com.sm.jeyz9.storemateapi.dto.RefundRequestDTO;
import com.sm.jeyz9.storemateapi.dto.ReorderRequestDTO;
import com.sm.jeyz9.storemateapi.dto.RetryPaymentRequestDTO;
import com.sm.jeyz9.storemateapi.dto.RetryPaymentResponseDTO;
import com.sm.jeyz9.storemateapi.exceptions.WebException;
import com.sm.jeyz9.storemateapi.models.CartItem;
import com.sm.jeyz9.storemateapi.models.CheckoutTypeName;
import com.sm.jeyz9.storemateapi.models.NotifyTypeName;
import com.sm.jeyz9.storemateapi.models.Order;
import com.sm.jeyz9.storemateapi.models.OrderAddress;
import com.sm.jeyz9.storemateapi.models.OrderChannelName;
import com.sm.jeyz9.storemateapi.models.OrderItem;
import com.sm.jeyz9.storemateapi.models.OrderStatusName;
import com.sm.jeyz9.storemateapi.models.Product;
import com.sm.jeyz9.storemateapi.models.RefundRequest;
import com.sm.jeyz9.storemateapi.models.RefundStatusName;
import com.sm.jeyz9.storemateapi.models.User;
import com.sm.jeyz9.storemateapi.models.UserAddress;
import com.sm.jeyz9.storemateapi.repository.CartItemRepository;
import com.sm.jeyz9.storemateapi.repository.OrderAddressRepository;
import com.sm.jeyz9.storemateapi.repository.OrderItemRepository;
import com.sm.jeyz9.storemateapi.repository.OrderRepository;
import com.sm.jeyz9.storemateapi.repository.ProductRepository;
import com.sm.jeyz9.storemateapi.repository.RefundRequestRepository;
import com.sm.jeyz9.storemateapi.repository.UserAddressRepository;
import com.sm.jeyz9.storemateapi.repository.UserRepository;
import com.sm.jeyz9.storemateapi.utils.RunningNumberUtil;
import com.stripe.Stripe;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
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
    private final MessagingService messagingService;
    private final RefundRequestRepository refundRequestRepository;
    private final LineMessageService lineMessageService;

    @Value("${stripe.secret-key}")
    private String secretKey;

    @Autowired
    public PaymentService(UserRepository userRepository, CartItemRepository cartItemRepository, OrderRepository orderRepository, OrderItemRepository orderItemRepository, UserAddressRepository userAddressRepository, OrderAddressRepository orderAddressRepository, ProductRepository productRepository, MessagingService messagingService, RefundRequestRepository refundRequestRepository, LineMessageService lineMessageService) {
        this.userRepository = userRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.userAddressRepository = userAddressRepository;
        this.orderAddressRepository = orderAddressRepository;
        this.productRepository = productRepository;
        this.messagingService = messagingService;
        this.refundRequestRepository = refundRequestRepository;
        this.lineMessageService = lineMessageService;
    }

    @Transactional
    public Map<String, String> checkoutIntent(String email, CheckoutRequestDTO request) {
        try {

            User user = userRepository.findUserByEmail(email)
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "User not found"));

            OrderStatusName status =
                    request.getCheckoutType().equals(CheckoutTypeName.DESTINATION)
                            ? OrderStatusName.PROCESSING
                            : OrderStatusName.PENDING;

            Order order = createOrder(user, request.getCheckoutType(), status);

            List<OrderItem> orderItems = request.getIds().stream().map(id -> {

                CartItem cartItem = cartItemRepository.findById(id)
                        .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "Cart item not found"));

                if (!cartItem.getCart().getUser().getId().equals(user.getId())) {
                    throw new WebException(HttpStatus.BAD_REQUEST, "Create order fail");
                }
                
                Product product = cartItem.getProduct();

                if (
                        !(product.getStock_quantity() > 0
                                && product.getProductStatus().getId().equals(1L)
                                && product.getStock_quantity() >= cartItem.getQuantity())
                ) {
                    throw new WebException(
                            HttpStatus.BAD_REQUEST,
                            "Product is unavailable"
                    );
                }
                
                product.setStock_quantity(product.getStock_quantity() - cartItem.getQuantity());
                productRepository.save(product);
                
                cartItemRepository.delete(cartItem);
                
                return OrderItem.builder()
                        .product(cartItem.getProduct())
                        .quantity(cartItem.getQuantity())
                        .order(order)
                        .unitPrice(cartItem.getProduct().getPrice())
                        .build();

            }).toList();

            orderItemRepository.saveAll(orderItems);
            
            double totalPrice = orderItems.stream().mapToDouble(o -> o.getUnitPrice() * o.getQuantity()).sum();
            order.setTotalPrice(totalPrice);

            handleCreateOrderAddress(user, order);

            Map<String, String> res = new HashMap<>();

            if (
                    request.getCheckoutType().equals(CheckoutTypeName.CARD)
                            || request.getCheckoutType().equals(CheckoutTypeName.PROMPTPAY)
            ) {

                long total = (long) (
                        orderItems.stream()
                                .mapToDouble(i -> i.getProduct().getPrice() * i.getQuantity())
                                .sum() * 100
                );

                if (total < 1) {
                    throw new WebException(HttpStatus.BAD_REQUEST, "Invalid total amount");
                }

                PaymentIntent intent = handleStripeIntent(total, user.getEmail());

                order.setStripePaymentIntent(intent.getId());
                order.setClientSecret(intent.getClientSecret());

                orderRepository.save(order);

                res.put("orderNo", order.getOrderNo());
                res.put("clientSecret", intent.getClientSecret());
                res.put("paymentIntentId", intent.getId());

                return res;
            }

            res.put("message", "create order success");
            res.put("orderNo", order.getOrderNo());

            return res;

        } catch (WebException e) {
            throw e;
        } catch (Exception e) {
            throw new WebException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Server Error: " + e.getMessage()
            );
        }
    }

    @Transactional
    public Map<String, String> checkoutNow(String email, CheckoutNowRequestDTO request) {
        try {

            User user = userRepository.findUserByEmail(email)
                    .orElseThrow(() ->
                            new WebException(HttpStatus.NOT_FOUND, "User not found")
                    );

            Product product = productRepository.findById(request.getId())
                    .orElseThrow(() ->
                            new WebException(HttpStatus.NOT_FOUND, "Product not found")
                    );

            if (
                    !(product.getStock_quantity() > 0
                            && product.getProductStatus().getId().equals(1L)
                            && product.getStock_quantity() >= request.getQuantity())
            ) {
                throw new WebException(
                        HttpStatus.BAD_REQUEST,
                        "Product is unavailable"
                );
            }

            OrderStatusName status =
                    request.getCheckoutType().equals(CheckoutTypeName.DESTINATION)
                            ? OrderStatusName.PROCESSING
                            : OrderStatusName.PENDING;

            Order order = createOrder(
                    user,
                    request.getCheckoutType(),
                    status
            );

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .quantity(request.getQuantity())
                    .order(order)
                    .unitPrice(product.getPrice())
                    .build();

            orderItemRepository.save(orderItem);

            product.setStock_quantity(product.getStock_quantity() - request.getQuantity());
            productRepository.save(product);

            double totalPrice = orderItem.getUnitPrice() * orderItem.getQuantity();
            order.setTotalPrice(totalPrice);

            handleCreateOrderAddress(user, order);

            Map<String, String> res = new HashMap<>();

            if (
                    request.getCheckoutType().equals(CheckoutTypeName.CARD)
                            || request.getCheckoutType().equals(CheckoutTypeName.PROMPTPAY)
            ) {

                long total = (long) (
                        product.getPrice() * request.getQuantity() * 100
                );

                if (total < 1) {
                    throw new WebException(
                            HttpStatus.BAD_REQUEST,
                            "Invalid total amount"
                    );
                }

                PaymentIntent intent = handleStripeIntent(
                        total,
                        user.getEmail()
                );

                order.setStripePaymentIntent(intent.getId());
                order.setClientSecret(intent.getClientSecret());

                orderRepository.save(order);

                res.put("orderNo", order.getOrderNo());
                res.put("clientSecret", intent.getClientSecret());
                res.put("paymentIntentId", intent.getId());

                return res;
            }

            res.put("message", "create order success");
            res.put("orderNo", order.getOrderNo());

            return res;

        } catch (WebException e) {
            throw e;
        } catch (Exception e) {
            throw new WebException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Server Error: " + e.getMessage()
            );
        }
    }
    
    @Transactional
    public String sendRefundRequest(RefundRequestDTO request, String email) {
        User user = userRepository.findUserByEmail(email).orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "User not found"));
        Order order = orderRepository.findOrderByOrderNoAndUserId(request.getOrderNo(), user.getId()).orElseThrow(() -> new WebException(HttpStatus.FORBIDDEN, "This order does not belong to you"));
        if(!order.getStatus().equals(OrderStatusName.PROCESSING) && !order.getStatus().equals(OrderStatusName.PENDING)) {
            throw new WebException(HttpStatus.BAD_REQUEST, "Can't refund this order");
        }
        
        if(order.getStatus().equals(OrderStatusName.PENDING)) {
            order.setStatus(OrderStatusName.CANCELLED);
            orderRepository.save(order);
            return "Cancel order success";
        }
        
        boolean existRefund = refundRequestRepository.existsByStatusAndOrderId(RefundStatusName.PENDING.name(), order.getId());
        if(existRefund) {
            throw new WebException(HttpStatus.BAD_REQUEST, "Refund exist");
        }
        
        RefundRequest refund = RefundRequest.builder()
                .id(null)
                .order(order)
                .user(user)
                .status(RefundStatusName.PENDING)
                .reason(request.getReason())
                .description(request.getDescription())
                .requestedAt(LocalDateTime.now())
                .refundNo(RunningNumberUtil.generate("REF"))
                .build();
        refundRequestRepository.save(refund);

        NotificationDTO notify = NotificationDTO.builder()
                .title("ส่งคำขอคืนเงินสำเร็จ")
                .message("คำขอคืนเงินสำหรับออร์เดอร์เลขที่ %s ถูกส่งให้เจ้าหน้าที่ดำเนินการเรียบร้อยแล้ว".formatted(order.getOrderNo()))
                .notifyType(NotifyTypeName.REFUNDED)
                .build();
        messagingService.sendNotifyToUser(email, notify);
        return "Send refund successfully";
    }
    
    @Transactional
    public String refundApprove(String refundNo) {
        try {
            RefundRequest refundRequest = refundRequestRepository.findOneByRefundNo(refundNo).orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "Refund request not found"));

            if (!refundRequest.getStatus().equals(RefundStatusName.PENDING)) {
                throw new WebException(HttpStatus.BAD_REQUEST, "This refund request can't approved");
            }
            refundRequest.setApprovedAt(LocalDateTime.now());
            refundRequest.setStatus(RefundStatusName.APPROVED);
            refundRequestRepository.save(refundRequest);

            Order order = orderRepository.findById(refundRequest.getOrder().getId()).orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "Order not found"));

            NotificationDTO notify = NotificationDTO.builder()
                    .title("คำขอคืนเงินได้รับการอนุมัติแล้ว")
                    .message("คำขอคืนเงินสำหรับออร์เดอร์เลขที่ %s ได้รับการอนุมัติเรียบร้อยแล้ว"
                            .formatted(order.getOrderNo()))
                    .notifyType(NotifyTypeName.REFUNDED)
                    .build();

            messagingService.sendNotifyToUser(refundRequest.getUser().getEmail(), notify);
            
            if (refundRequest.getOrder().getCheckoutType().equals(CheckoutTypeName.DESTINATION)) {
                order.setStatus(OrderStatusName.CANCELLED);
                orderRepository.save(order);
                return "Approve refund request success";
            }

            long amount = (long) (refundRequest.getOrder().getOrderItems().stream().mapToDouble(oi -> oi.getProduct().getPrice() * oi.getQuantity()).sum() * 100);

            try {
                Stripe.apiKey = secretKey;
                RefundCreateParams params = RefundCreateParams.builder()
                        .setPaymentIntent(refundRequest.getOrder().getStripePaymentIntent())
                        .setAmount(amount)
                        .setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER)
                        .build();
                Refund.create(params);
            } catch (StripeException e) {
                throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            }
            order.setStatus(OrderStatusName.REFUNDED);
            orderRepository.save(order);
            return "Approve refund request success";
        }catch(Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error: " + e.getMessage());
        }
    }

    public RetryPaymentResponseDTO retryPayment(RetryPaymentRequestDTO request) throws Exception {

        Order order = orderRepository.findOneByOrderNo(request.getOrderNo()).orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "Order not found"));
        PaymentIntent paymentIntent =
                PaymentIntent.retrieve(order.getStripePaymentIntent());

        if ("requires_payment_method".equals(paymentIntent.getStatus())
                || "requires_confirmation".equals(paymentIntent.getStatus())
                || "requires_action".equals(paymentIntent.getStatus())) {

            return RetryPaymentResponseDTO.builder()
                    .clientSecret(paymentIntent.getClientSecret())
                    .paymentIntentId(paymentIntent.getId())
                    .build();
        }

        PaymentIntentCreateParams params =
                PaymentIntentCreateParams.builder()
                        .setAmount(paymentIntent.getAmount())
                        .setCurrency(paymentIntent.getCurrency())
                        .build();

        PaymentIntent newPaymentIntent = PaymentIntent.create(params);
        
        order.setStripePaymentIntent(newPaymentIntent.getId());
        order.setClientSecret(newPaymentIntent.getClientSecret());
        orderRepository.save(order);

        return RetryPaymentResponseDTO.builder()
                .clientSecret(newPaymentIntent.getClientSecret())
                .paymentIntentId(newPaymentIntent.getId())
                .build();
    }

    public String refundReject(String refundNo) {
        RefundRequest refundRequest = refundRequestRepository.findOneByRefundNo(refundNo).orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "Refund request not found"));
        if(!refundRequest.getStatus().equals(RefundStatusName.PENDING)) {
            throw new WebException(HttpStatus.BAD_REQUEST, "This refund request can't rejected");
        }

        NotificationDTO notify = NotificationDTO.builder()
                .title("คำขอคืนเงินถูกปฏิเสธ")
                .message("คำขอคืนเงินสำหรับออร์เดอร์เลขที่ %s ถูกปฏิเสธ".formatted(refundRequest.getOrder().getOrderNo()))
                .notifyType(NotifyTypeName.REFUNDED)
                .build();

        messagingService.sendNotifyToUser(refundRequest.getUser().getEmail(), notify);
        refundRequest.setStatus(RefundStatusName.REJECTED);
        refundRequestRepository.save(refundRequest);
        return "Reject refund request success";
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
    
    @Transactional
    public Map<String, String> reorder(ReorderRequestDTO request, String email) {
        Order order = orderRepository.findOneByOrderNo(request.getOrderNo()).orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "Order not found"));
        try {
            User user = userRepository.findUserByEmail(email)
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "User not found"));

            OrderStatusName status =
                    request.getCheckoutType().equals(CheckoutTypeName.DESTINATION)
                            ? OrderStatusName.PROCESSING
                            : OrderStatusName.PENDING;

            Order createOrder = createOrder(user, order.getCheckoutType(), status);

            List<OrderItem> orderItems = order.getOrderItems().stream().map(item -> {

                Product product = item.getProduct();

                if (
                        !(product.getStock_quantity() > 0
                                && product.getProductStatus().getId().equals(1L)
                                && product.getStock_quantity() >= item.getQuantity())
                ) {
                    throw new WebException(
                            HttpStatus.BAD_REQUEST,
                            "Product is unavailable"
                    );
                }

                product.setStock_quantity(product.getStock_quantity() - item.getQuantity());
                productRepository.save(product);

                return OrderItem.builder()
                        .product(item.getProduct())
                        .quantity(item.getQuantity())
                        .order(createOrder)
                        .unitPrice(item.getProduct().getPrice())
                        .build();

            }).toList();

            orderItemRepository.saveAll(orderItems);

            double totalPrice = orderItems.stream().mapToDouble(o -> o.getUnitPrice() * o.getQuantity()).sum();
            order.setTotalPrice(totalPrice);

            handleCreateOrderAddress(user, order);

            Map<String, String> res = new HashMap<>();

            if (
                    order.getCheckoutType().equals(CheckoutTypeName.CARD)
                            || order.getCheckoutType().equals(CheckoutTypeName.PROMPTPAY)
            ) {

                long total = (long) (
                        orderItems.stream()
                                .mapToDouble(i -> i.getProduct().getPrice() * i.getQuantity())
                                .sum() * 100
                );

                if (total < 1) {
                    throw new WebException(HttpStatus.BAD_REQUEST, "Invalid total amount");
                }

                PaymentIntent intent = handleStripeIntent(total, user.getEmail());

                order.setStripePaymentIntent(intent.getId());
                order.setClientSecret(intent.getClientSecret());

                orderRepository.save(order);

                res.put("orderNo", order.getOrderNo());
                res.put("clientSecret", intent.getClientSecret());
                res.put("paymentIntentId", intent.getId());

                return res;
            }

            res.put("message", "create order success");
            res.put("orderNo", order.getOrderNo());

            return res;

        } catch (WebException e) {
            throw e;
        } catch (Exception e) {
            throw new WebException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Server Error: " + e.getMessage()
            );
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

            messagingService.sendToUser(order.getUser().getEmail(), "PAYMENT_FAILED");

            orderRepository.save(order);
        }
    }
    
    private void markAsPaid(String clientSecret) {
        Order order = orderRepository.findByStripePaymentIntent(clientSecret).orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "Intent not found"));
        order.setPaidAt(LocalDateTime.now());
        order.setStatus(OrderStatusName.PROCESSING);
        messagingService.sendToUser(order.getUser().getEmail(), "PAYMENT_SUCCESS");
        orderRepository.save(order);

        String lineUserId = order.getUser().getLineUserId();
        if (lineUserId != null) {
            double total = order.getOrderItems().stream()
                    .mapToDouble(i -> i.getProduct().getPrice() * i.getQuantity()).sum();
            lineMessageService.pushPaymentSuccess(lineUserId, order.getOrderNo(), total);
        }
    }
    
    private PaymentIntent handleStripeIntent(Long total, String email) throws StripeException {
        Stripe.apiKey = secretKey;
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(total)
                .setCurrency("thb")
                .addPaymentMethodType("card")
                .addPaymentMethodType("promptpay")
                .setReceiptEmail(email)
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
                .recipientName(user.getName())
                .phone(user.getPhone())
                .build();
        
        orderAddressRepository.save(orderAddress);
    }

    private Order createOrder(User user, CheckoutTypeName checkoutType, OrderStatusName status) {
        Order order = Order.builder()
                .orderNo(RunningNumberUtil.generate("ORD"))
                .status(status)
                .user(user)
                .createdAt(LocalDateTime.now())
                .orderChannel(OrderChannelName.WEBSITE)
                .checkoutType(checkoutType)
                .build();

        return orderRepository.save(order);
    }
}
