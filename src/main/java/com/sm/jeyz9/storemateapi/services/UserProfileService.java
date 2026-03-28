package com.sm.jeyz9.storemateapi.services;

import com.sm.jeyz9.storemateapi.dto.UserAddressDTO;
import com.sm.jeyz9.storemateapi.dto.UserAddressRequestDTO;
import com.sm.jeyz9.storemateapi.dto.UserProfileRequestDTO;
import com.sm.jeyz9.storemateapi.models.User;
import com.sm.jeyz9.storemateapi.models.UserAddress;

import java.util.List;

public interface UserProfileService {
    UserProfileRequestDTO getUserProfile(String email);
    User updateProfile(String email, UserProfileRequestDTO dto, org.springframework.web.multipart.MultipartFile image);
    UserAddressDTO addUserAddress(String email, UserAddressRequestDTO dto);
    List<UserAddressDTO> getUserAddresses(String email);
    UserAddressDTO getUserAddressById(Long addressId, String email);
    void deleteUserAddress(Long addressId, String email);
}