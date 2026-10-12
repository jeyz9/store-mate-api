package com.sm.jeyz9.storemateapi.repository;

import java.time.LocalDateTime;

public interface ProductCategoryProjection {
    Long getId();
    String getProductName();
    String getCategoryName();
    String getDescription();
    String getImageUrl();
    Double getPrice();
    LocalDateTime getCreatedAt();
    String getProductStatus();
    Boolean getDeleted();
}
