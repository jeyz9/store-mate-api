package com.sm.jeyz9.storemateapi;

import com.sm.jeyz9.storemateapi.exceptions.WebException;
import com.sm.jeyz9.storemateapi.models.Order;
import com.sm.jeyz9.storemateapi.repository.OrderRepository;
import com.sm.jeyz9.storemateapi.repository.OrderStatusHistoryRepository;
import com.sm.jeyz9.storemateapi.repository.RefundRequestRepository;
import com.sm.jeyz9.storemateapi.repository.ReviewRepository;
import com.sm.jeyz9.storemateapi.repository.StoreInfoRepository;
import com.sm.jeyz9.storemateapi.repository.UserRepository;
import com.sm.jeyz9.storemateapi.services.MessagingService;
import com.sm.jeyz9.storemateapi.services.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderServiceTest {

    private RecordingOrderRepository orderRepository;
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderRepository = new RecordingOrderRepository();
        orderService = new OrderServiceImpl(
                orderRepository.asRepository(),
                proxy(UserRepository.class, (proxy, method, args) -> null),
                proxy(OrderStatusHistoryRepository.class, (proxy, method, args) -> null),
                new ModelMapper(),
                proxy(StoreInfoRepository.class, (proxy, method, args) -> null),
                proxy(RefundRequestRepository.class, (proxy, method, args) -> null),
                proxy(ReviewRepository.class, (proxy, method, args) -> null),
                new MessagingService(null, null, null, null)
        );
    }

    @Test
    void getAllOrdersConvertsDateFiltersAndAppliesPagination() {
        LocalDate startDate = LocalDate.of(2026, 4, 2);
        LocalDate endDate = LocalDate.of(2026, 4, 8);

        Page<?> result = orderService.getAllOrders(
                "recipient", startDate, endDate, null, 2, 15
        );

        assertEquals(0, result.getTotalElements());
        assertEquals("recipient", orderRepository.keyword);
        assertEquals(startDate.atStartOfDay(), orderRepository.start);
        assertEquals(endDate.atTime(LocalTime.MAX), orderRepository.end);
        assertEquals(2, orderRepository.pageable.getPageNumber());
        assertEquals(15, orderRepository.pageable.getPageSize());
        assertEquals(1, orderRepository.callCount);
    }

    @Test
    void getAllOrdersRejectsUnsupportedPeriodBeforeCallingRepository() {
        WebException exception = assertThrows(WebException.class, () ->
                orderService.getAllOrders(null, null, null, "YEAR", 0, 10)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Invalid period", exception.getMessage());
        assertEquals(0, orderRepository.callCount);
    }

    private static class RecordingOrderRepository implements InvocationHandler {
        private String keyword;
        private LocalDateTime start;
        private LocalDateTime end;
        private Pageable pageable;
        private int callCount;

        private OrderRepository asRepository() {
            return proxy(OrderRepository.class, this);
        }

        @Override
        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
            if (method.getName().equals("findOrders")) {
                keyword = (String) args[0];
                start = (LocalDateTime) args[1];
                end = (LocalDateTime) args[2];
                pageable = (Pageable) args[3];
                callCount++;
                return new PageImpl<>(List.<Order>of(), pageable, 0);
            }
            if (method.getName().equals("findOrderStatusByOrderNo")) {
                return null;
            }
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }
}
