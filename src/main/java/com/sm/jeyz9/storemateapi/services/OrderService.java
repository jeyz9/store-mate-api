package com.sm.jeyz9.storemateapi.services;

import com.sm.jeyz9.storemateapi.dto.OrderDTO;
import com.sm.jeyz9.storemateapi.dto.OrderDetailsDTO;
import com.sm.jeyz9.storemateapi.models.OrderStatusName;

import java.util.List;

public interface OrderService {
    List<OrderDTO> getUserOrders(OrderStatusName status, String email);
    String getOrderStatus(String orderNo);
    OrderDetailsDTO getOrderDetails(String email, String orderNo);
    void getAllOrders();
    void getOrderDetailsByModerator();
    void changeOrderStatus();
    void printShippingLabel();
}
