package com.sm.jeyz9.storemateapi.services;

import com.sm.jeyz9.storemateapi.dto.StoreInfoDTO;
import com.sm.jeyz9.storemateapi.dto.StoreInfoRequestDTO;
import org.springframework.web.multipart.MultipartFile;

public interface StoreInfoService {
    StoreInfoDTO getStoreDetails();
    StoreInfoDTO updateStoreDetails(Long storeId, StoreInfoRequestDTO request, String email ,MultipartFile image);
}