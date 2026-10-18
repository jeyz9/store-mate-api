package com.sm.jeyz9.storemateapi;

import com.sm.jeyz9.storemateapi.dto.RetryPaymentRequestDTO;
import com.sm.jeyz9.storemateapi.dto.ReorderRequestDTO;
import com.sm.jeyz9.storemateapi.exceptions.WebException;
import com.sm.jeyz9.storemateapi.models.CheckoutTypeName;
import com.sm.jeyz9.storemateapi.models.Order;
import com.sm.jeyz9.storemateapi.models.OrderAddress;
import com.sm.jeyz9.storemateapi.models.OrderItem;
import com.sm.jeyz9.storemateapi.models.OrderStatusName;
import com.sm.jeyz9.storemateapi.models.Product;
import com.sm.jeyz9.storemateapi.models.ProductStatus;
import com.sm.jeyz9.storemateapi.models.User;
import com.sm.jeyz9.storemateapi.models.UserAddress;
import com.sm.jeyz9.storemateapi.models.Zipcode;
import com.sm.jeyz9.storemateapi.repository.CartItemRepository;
import com.sm.jeyz9.storemateapi.repository.OrderAddressRepository;
import com.sm.jeyz9.storemateapi.repository.OrderItemRepository;
import com.sm.jeyz9.storemateapi.repository.OrderRepository;
import com.sm.jeyz9.storemateapi.repository.ProductRepository;
import com.sm.jeyz9.storemateapi.repository.ProductStatusRepository;
import com.sm.jeyz9.storemateapi.repository.RefundRequestRepository;
import com.sm.jeyz9.storemateapi.repository.UserAddressRepository;
import com.sm.jeyz9.storemateapi.repository.UserRepository;
import com.sm.jeyz9.storemateapi.services.LineMessageService;
import com.sm.jeyz9.storemateapi.services.MessagingService;
import com.sm.jeyz9.storemateapi.services.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentServiceTest {

    private RecordingUserRepository userRepository;
    private RecordingOrderRepository orderRepository;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        userRepository = new RecordingUserRepository();
        orderRepository = new RecordingOrderRepository();
        paymentService = new PaymentService(
                userRepository.asRepository(),
                proxy(CartItemRepository.class, (proxy, method, args) -> null),
                orderRepository.asRepository(),
                proxy(OrderItemRepository.class, (proxy, method, args) -> null),
                proxy(UserAddressRepository.class, (proxy, method, args) -> null),
                proxy(OrderAddressRepository.class, (proxy, method, args) -> null),
                proxy(ProductRepository.class, (proxy, method, args) -> null),
                new MessagingService(null, null, null, null),
                proxy(RefundRequestRepository.class, (proxy, method, args) -> null),
                proxy(LineMessageService.class, (proxy, method, args) -> null),
                proxy(ProductStatusRepository.class, (proxy, method, args) -> null)
        );
    }

    @Test
    void checkoutIntentReturnsNotFoundWhenUserDoesNotExist() {
        WebException exception = assertThrows(WebException.class, () ->
                paymentService.checkoutIntent("missing@example.com", null)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("User not found", exception.getMessage());
        assertEquals("missing@example.com", userRepository.requestedEmail);
        assertEquals(0, orderRepository.findByOrderNumberCallCount);
    }

    @Test
    void retryPaymentReturnsNotFoundWhenOrderDoesNotExist() {
        RetryPaymentRequestDTO request = new RetryPaymentRequestDTO();
        request.setOrderNo("ORD-404");

        WebException exception = assertThrows(WebException.class, () ->
                paymentService.retryPayment(request)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("Order not found", exception.getMessage());
        assertEquals("ORD-404", orderRepository.requestedOrderNo);
        assertEquals(1, orderRepository.findByOrderNumberCallCount);
    }

    @Test
    void reorderSavesTotalAndShippingAddressOnTheNewOrderUsingRequestedCheckoutType() {
        User user = User.builder().id(7L).email("buyer@example.com").name("Buyer").phone("0812345678").build();
        Product product = Product.builder()
                .id(12L)
                .name("Soap")
                .price(200.0)
                .stock_quantity(5)
                .productStatus(new ProductStatus(1L, "ACTIVE"))
                .build();
        OrderItem oldItem = OrderItem.builder().product(product).quantity(2).unitPrice(150.0).build();
        Order oldOrder = Order.builder()
                .id(99L)
                .orderNo("ORD-OLD")
                .user(user)
                .checkoutType(CheckoutTypeName.CARD)
                .orderItems(java.util.Set.of(oldItem))
                .build();
        UserAddress userAddress = UserAddress.builder()
                .id(3L)
                .user(user)
                .streetAddress("123 Main Road")
                .zipcode(new Zipcode())
                .isDefault(true)
                .build();

        Order[] savedOrder = new Order[1];
        OrderAddress[] savedAddress = new OrderAddress[1];
        var users = proxy(UserRepository.class, (proxy, method, args) ->
                method.getName().equals("findUserByEmail") ? Optional.of(user) : null);
        var orders = proxy(OrderRepository.class, (proxy, method, args) -> switch (method.getName()) {
            case "findOneByOrderNo" -> Optional.of(oldOrder);
            case "save" -> {
                savedOrder[0] = (Order) args[0];
                yield args[0];
            }
            default -> null;
        });
        var orderItems = proxy(OrderItemRepository.class, (proxy, method, args) ->
                method.getName().equals("saveAll") ? args[0] : null);
        var userAddresses = proxy(UserAddressRepository.class, (proxy, method, args) ->
                method.getName().equals("findByUserIdAndIsDefaultTrue") ? Optional.of(userAddress) : null);
        var orderAddresses = proxy(OrderAddressRepository.class, (proxy, method, args) -> {
            if (method.getName().equals("save")) {
                savedAddress[0] = (OrderAddress) args[0];
                return args[0];
            }
            return null;
        });
        PaymentService service = new PaymentService(
                users,
                proxy(CartItemRepository.class, (proxy, method, args) -> null),
                orders,
                orderItems,
                userAddresses,
                orderAddresses,
                proxy(ProductRepository.class, (proxy, method, args) -> args[0]),
                new MessagingService(null, null, null, null),
                proxy(RefundRequestRepository.class, (proxy, method, args) -> null),
                proxy(LineMessageService.class, (proxy, method, args) -> null),
                proxy(ProductStatusRepository.class, (proxy, method, args) -> null)
        );
        ReorderRequestDTO request = new ReorderRequestDTO();
        request.setOrderNo("ORD-OLD");
        request.setCheckoutType(CheckoutTypeName.DESTINATION);

        service.reorder(request, user.getEmail());

        assertSame(savedOrder[0], savedAddress[0].getOrder());
        assertEquals(CheckoutTypeName.DESTINATION, savedOrder[0].getCheckoutType());
        assertEquals(OrderStatusName.PROCESSING, savedOrder[0].getStatus());
        assertEquals(400.0, savedOrder[0].getTotalPrice());
        assertEquals("Buyer", savedAddress[0].getRecipientName());
        assertEquals("0812345678", savedAddress[0].getPhone());
        assertEquals("123 Main Road", savedAddress[0].getStreetAddress());
        assertEquals(null, oldOrder.getTotalPrice());
    }

    private static class RecordingUserRepository implements InvocationHandler {
        private String requestedEmail;

        private UserRepository asRepository() {
            return proxy(UserRepository.class, this);
        }

        @Override
        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
            if (method.getName().equals("findUserByEmail")) {
                requestedEmail = (String) args[0];
                return Optional.empty();
            }
            return null;
        }
    }

    private static class RecordingOrderRepository implements InvocationHandler {
        private String requestedOrderNo;
        private int findByOrderNumberCallCount;

        private OrderRepository asRepository() {
            return proxy(OrderRepository.class, this);
        }

        @Override
        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
            if (method.getName().equals("findOneByOrderNo")) {
                requestedOrderNo = (String) args[0];
                findByOrderNumberCallCount++;
                return Optional.empty();
            }
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }
}
