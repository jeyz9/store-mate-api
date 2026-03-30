package com.sm.jeyz9.storemateapi.services;

import com.sm.jeyz9.storemateapi.dto.UserAddressDTO;
import com.sm.jeyz9.storemateapi.dto.UserAddressRequestDTO;
import com.sm.jeyz9.storemateapi.dto.UserProfileRequestDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserProfileService {
    UserProfileRequestDTO getUserProfile(String email);
    String updateProfile(String email, UserProfileRequestDTO dto, MultipartFile image);
    
    UserAddressDTO addUserAddress(String email, UserAddressRequestDTO dto);
    List<UserAddressDTO> getUserAddresses(String email);
    UserAddressDTO getUserAddressById(Long addressId, String email);
    void deleteUserAddress(Long addressId, String email);
    UserAddressDTO updateUserAddress(Long addressId, UserAddressRequestDTO dto, String email);
    UserAddressDTO setDefaultAddress(Long addressId, String email);
    UserAddressDTO getDefaultAddress(String email);
}