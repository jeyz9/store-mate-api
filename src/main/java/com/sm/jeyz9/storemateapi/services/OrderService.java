package com.sm.jeyz9.storemateapi.services;

import com.sm.jeyz9.storemateapi.dto.OrderDTO;
import com.sm.jeyz9.storemateapi.models.OrderStatusName;

import java.util.List;

public interface OrderService {
    List<OrderDTO> getUserOrders(OrderStatusName status, String email);
    String getOrderStatus(Long orderId);
    void getOrderDetails();
    void getAllOrders();
    void getOrderDetailsByModerator();
    void changeOrderStatus();
    void printShippingLabel();
}
