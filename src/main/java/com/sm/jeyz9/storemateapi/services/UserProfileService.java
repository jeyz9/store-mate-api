package com.sm.jeyz9.storemateapi.services;

import com.sm.jeyz9.storemateapi.dto.UserProfileRequestDTO;
import com.sm.jeyz9.storemateapi.models.User;

public interface UserProfileService {
    UserProfileRequestDTO getUserProfile(String email);
}