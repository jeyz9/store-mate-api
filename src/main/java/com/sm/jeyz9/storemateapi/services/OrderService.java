package com.sm.jeyz9.storemateapi.services;

import com.sm.jeyz9.storemateapi.dto.OrderDTO;
import com.sm.jeyz9.storemateapi.dto.OrderDetailsDTO;
import com.sm.jeyz9.storemateapi.dto.OrderModDTO;
import com.sm.jeyz9.storemateapi.dto.OrderModDetailsDTO;
import com.sm.jeyz9.storemateapi.dto.ShippingDTO;
import com.sm.jeyz9.storemateapi.models.OrderStatusName;

import java.time.LocalDate;
import java.util.List;

public interface OrderService {
    List<OrderDTO> getUserOrders(OrderStatusName status, String email);
    String getOrderStatus(String orderNo);
    OrderDetailsDTO getOrderDetails(String email, String orderNo);
    List<OrderModDTO> getAllOrders(String keyword, LocalDate startDate, LocalDate endDate, String period);
    OrderModDetailsDTO getOrderDetailsByModerator(String orderNo);
    String changeOrderStatus(String orderNo, String status, String email);
    List<ShippingDTO> printShippingLabel(List<Long> ids);
}
