package com.sm.jeyz9.storemateapi.services.impl;

import com.sm.jeyz9.storemateapi.exceptions.WebException;
import com.sm.jeyz9.storemateapi.models.*;
import com.sm.jeyz9.storemateapi.repository.*;
import com.sm.jeyz9.storemateapi.services.LinePaymentService;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LinePaymentServiceImpl implements LinePaymentService {

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderAddressRepository orderAddressRepository;
    private final UserAddressRepository userAddressRepository;

    @Autowired
    public LinePaymentServiceImpl(
            UserRepository userRepository,
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderAddressRepository orderAddressRepository,
            UserAddressRepository userAddressRepository) {
        this.userRepository         = userRepository;
        this.cartRepository         = cartRepository;
        this.cartItemRepository     = cartItemRepository;
        this.orderRepository        = orderRepository;
        this.orderItemRepository    = orderItemRepository;
        this.orderAddressRepository = orderAddressRepository;
        this.userAddressRepository  = userAddressRepository;
    }

    @Override
    @Transactional
    public LineCheckoutResult createPromptPayCheckout(Long userId) {
        // 1. หา User
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบข้อมูลผู้ใช้"));

        // 2. หา Active cart
        Cart cart = cartRepository.findCartByStatusAndUserId(CartStatusName.ACTIVE, userId)
                .orElseThrow(() -> new WebException(HttpStatus.BAD_REQUEST, "ไม่มีสินค้าในตะกร้า"));

        // 3. ดึงรายการสินค้า
        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            throw new WebException(HttpStatus.BAD_REQUEST, "ไม่มีสินค้าในตะกร้า");
        }

        // 4. ตรวจสอบที่อยู่จัดส่ง
        UserAddress address = userAddressRepository.findByUserIdAndIsDefaultTrue(userId)
                .orElseThrow(() -> new WebException(HttpStatus.BAD_REQUEST,
                        "กรุณาตั้งค่าที่อยู่จัดส่งก่อนสั่งซื้อ\n\nพิมพ์ \"จัดการที่อยู่\" เพื่อเพิ่มที่อยู่"));

        // 5. คำนวณยอดรวม (สตางค์ = บาท × 100)
        long totalSatang = (long) (cartItems.stream()
                .mapToDouble(ci -> ci.getProduct().getPrice() * ci.getQuantity())
                .sum() * 100);

        if (totalSatang < 1) {
            throw new WebException(HttpStatus.BAD_REQUEST, "ยอดรวมไม่ถูกต้อง");
        }

        try {
            // 6. สร้าง Stripe PaymentIntent แบบ PromptPay และ confirm ทันที
            Stripe.apiKey = stripeSecretKey;
            // Line user อาจไม่มี email → ใช้ placeholder แทน
            String email = (user.getEmail() != null && !user.getEmail().isBlank())
                    ? user.getEmail()
                    : "line_" + user.getId() + "@storematechat.com";

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(totalSatang)
                    .setCurrency("thb")
                    .addPaymentMethodType("promptpay")
                    .setPaymentMethodData(
                            PaymentIntentCreateParams.PaymentMethodData.builder()
                                    .setType(PaymentIntentCreateParams.PaymentMethodData.Type.PROMPTPAY)
                                    .setBillingDetails(
                                            PaymentIntentCreateParams.PaymentMethodData.BillingDetails.builder()
                                                    .setEmail(email)
                                                    .build()
                                    )
                                    .build()
                    )
                    .setConfirm(true)
                    .setReturnUrl("https://example.com/return")
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            // 7. ดึง QR code URL จาก next_action
            String qrCodeUrl = null;
            if (intent.getNextAction() != null
                    && intent.getNextAction().getPromptpayDisplayQrCode() != null) {
                qrCodeUrl = intent.getNextAction().getPromptpayDisplayQrCode().getImageUrlPng();
            }
            if (qrCodeUrl == null) {
                throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "ไม่สามารถสร้าง QR Code ได้");
            }

            // 8. สร้าง Order
            Order order = Order.builder()
                    .status(OrderStatusName.PENDING)
                    .user(user)
                    .checkoutType(CheckoutTypeName.PROMPTPAY)
                    .orderChannel(OrderChannelName.LINE_OA)
                    .stripePaymentIntent(intent.getId())
                    .clientSecret(intent.getClientSecret())
                    .createdAt(LocalDateTime.now())
                    .build();
            orderRepository.save(order);

            // 9. สร้าง OrderItems
            List<OrderItem> orderItems = cartItems.stream()
                    .map(ci -> OrderItem.builder()
                            .order(order)
                            .product(ci.getProduct())
                            .quantity(ci.getQuantity())
                            .build())
                    .toList();
            orderItemRepository.saveAll(orderItems);

            // 10. สร้าง OrderAddress จากที่อยู่ของ user
            OrderAddress orderAddress = OrderAddress.builder()
                    .order(order)
                    .streetAddress(address.getStreetAddress())
                    .zipcode(address.getZipcode())
                    .createdAt(LocalDateTime.now())
                    .build();
            orderAddressRepository.save(orderAddress);

            // 11. ล้างตะกร้า
            cartItemRepository.deleteAll(cartItems);
            cart.setStatus(CartStatusName.CHECKED_OUT);
            cartRepository.save(cart);

            double totalBaht = totalSatang / 100.0;
            return new LineCheckoutResult(order.getOrderNo(), qrCodeUrl, totalBaht);

        } catch (WebException e) {
            throw e;
        } catch (StripeException e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "Stripe error: " + e.getMessage());
        } catch (Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error: " + e.getMessage());
        }
    }

    @Override
    public String getQrCodeUrl(String stripePaymentIntentId) {
        try {
            Stripe.apiKey = stripeSecretKey;
            PaymentIntent intent = PaymentIntent.retrieve(stripePaymentIntentId);
            if (intent.getNextAction() != null
                    && intent.getNextAction().getPromptpayDisplayQrCode() != null) {
                return intent.getNextAction().getPromptpayDisplayQrCode().getImageUrlPng();
            }
            return null;
        } catch (StripeException e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "Stripe error: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public String renewQrCode(String oldPaymentIntentId) {
        // 1. หา order จาก PI เดิม
        Order order = orderRepository.findByStripePaymentIntent(oldPaymentIntentId)
                .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบรายการสั่งซื้อ"));

        if (order.getStatus() != OrderStatusName.PENDING) {
            throw new WebException(HttpStatus.BAD_REQUEST, "ออเดอร์นี้ไม่อยู่ในสถานะรอชำระเงิน");
        }

        // 2. คำนวณยอดจาก OrderItems เดิม
        long totalSatang = (long) (order.getOrderItems().stream()
                .mapToDouble(i -> i.getProduct().getPrice() * i.getQuantity())
                .sum() * 100);

        // 3. หา email สำหรับ Stripe
        User user = order.getUser();
        String email = (user.getEmail() != null && !user.getEmail().isBlank())
                ? user.getEmail()
                : "line_" + user.getId() + "@storematechat.com";

        try {
            // 4. สร้าง PI ใหม่
            Stripe.apiKey = stripeSecretKey;
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(totalSatang)
                    .setCurrency("thb")
                    .addPaymentMethodType("promptpay")
                    .setPaymentMethodData(
                            PaymentIntentCreateParams.PaymentMethodData.builder()
                                    .setType(PaymentIntentCreateParams.PaymentMethodData.Type.PROMPTPAY)
                                    .setBillingDetails(
                                            PaymentIntentCreateParams.PaymentMethodData.BillingDetails.builder()
                                                    .setEmail(email)
                                                    .build()
                                    )
                                    .build()
                    )
                    .setConfirm(true)
                    .setReturnUrl("https://example.com/return")
                    .build();

            PaymentIntent newIntent = PaymentIntent.create(params);

            // 5. ดึง QR URL
            String qrUrl = null;
            if (newIntent.getNextAction() != null
                    && newIntent.getNextAction().getPromptpayDisplayQrCode() != null) {
                qrUrl = newIntent.getNextAction().getPromptpayDisplayQrCode().getImageUrlPng();
            }
            if (qrUrl == null) {
                throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "ไม่สามารถสร้าง QR Code ได้");
            }

            // 6. อัปเดต order ด้วย PI ใหม่
            order.setStripePaymentIntent(newIntent.getId());
            order.setClientSecret(newIntent.getClientSecret());
            orderRepository.save(order);

            return qrUrl;

        } catch (WebException e) {
            throw e;
        } catch (StripeException e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "Stripe error: " + e.getMessage());
        }
    }
}
