package com.sm.jeyz9.storemateapi.services;

public interface OrderService {
    void getUserOrders();
    void getOrderDetails();
    void getAllOrders();
    void getOrderDetailsByModerator();
    void changeOrderStatus();
    void printShippingLabel();
}
