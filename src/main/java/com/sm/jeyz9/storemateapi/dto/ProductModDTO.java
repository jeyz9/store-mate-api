package com.sm.jeyz9.storemateapi.dto;

public interface ProductModDTO {
    Long getId();
    String getProductNo();
    String getProductName();
    String getCategory();
    double getPrice();
    int getStockQuantity();
    String getStatus();
}
