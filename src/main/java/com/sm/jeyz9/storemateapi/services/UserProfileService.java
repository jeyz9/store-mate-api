package com.sm.jeyz9.storemateapi.services;

import com.sm.jeyz9.storemateapi.dto.UserAddressRequestDTO;
import com.sm.jeyz9.storemateapi.dto.UserProfileRequestDTO;
import com.sm.jeyz9.storemateapi.models.User;
import com.sm.jeyz9.storemateapi.models.UserAddress;

public interface UserProfileService {
    UserProfileRequestDTO getUserProfile(String email);
    User updateProfile(String email, UserProfileRequestDTO dto, org.springframework.web.multipart.MultipartFile image);
    UserAddress addUserAddress(String email, UserAddressRequestDTO dto);
}