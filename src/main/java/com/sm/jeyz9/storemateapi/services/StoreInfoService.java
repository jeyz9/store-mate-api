package com.sm.jeyz9.storemateapi.services;

import com.sm.jeyz9.storemateapi.dto.StoreInfoDTO;
import com.sm.jeyz9.storemateapi.dto.StoreInfoRequestDTO;

public interface StoreInfoService {
    StoreInfoDTO getStoreDetails(String email);
    StoreInfoDTO updateStoreDetails(Long storeId, StoreInfoRequestDTO request, String email);
}