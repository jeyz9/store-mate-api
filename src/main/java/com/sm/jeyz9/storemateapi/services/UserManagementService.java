package com.sm.jeyz9.storemateapi.services;

import com.sm.jeyz9.storemateapi.dto.PaginationDTO;
import com.sm.jeyz9.storemateapi.dto.UserManagementDTO;
import com.sm.jeyz9.storemateapi.dto.UserRoleRequestDTO;

public interface UserManagementService {
    PaginationDTO<UserManagementDTO> getAllUsers(int page, int size, String email);
    UserManagementDTO updateUserRole(Long userId, UserRoleRequestDTO request, String email);
    UserManagementDTO suspendUser(Long userId, String email);
    UserManagementDTO activateUser(Long userId, String email);
}