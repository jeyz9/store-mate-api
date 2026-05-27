package com.sm.jeyz9.storemateapi.dto;

import com.sm.jeyz9.storemateapi.models.ShippingItemsDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShippingDTO {
    private String orderNo;
    private List<ShippingItemsDTO> shippingItems;
    private Double total;
    private String checkoutType;
    private PersonInfoDTO senderInfo;
    private PersonInfoDTO receiverInfo;
}
