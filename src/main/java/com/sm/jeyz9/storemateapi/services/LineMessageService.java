package com.sm.jeyz9.storemateapi.services;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.scheduling.annotation.Async;

public interface LineMessageService {
    @Async
    void handleEvent(JsonNode event);

    void pushPaymentSuccess(String lineUserId, String orderNo, double total);
}