package com.sm.jeyz9.storemateapi;

import com.sm.jeyz9.storemateapi.dto.RetryPaymentRequestDTO;
import com.sm.jeyz9.storemateapi.exceptions.WebException;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
