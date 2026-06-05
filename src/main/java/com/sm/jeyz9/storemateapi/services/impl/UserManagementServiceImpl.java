package com.sm.jeyz9.storemateapi.services.impl;

import com.sm.jeyz9.storemateapi.dto.PaginationDTO;
import com.sm.jeyz9.storemateapi.dto.UserManagementDTO;
import com.sm.jeyz9.storemateapi.dto.UserRoleRequestDTO;
import com.sm.jeyz9.storemateapi.exceptions.WebException;
import com.sm.jeyz9.storemateapi.models.RoleName;
import com.sm.jeyz9.storemateapi.models.Role;
import com.sm.jeyz9.storemateapi.models.User;
import com.sm.jeyz9.storemateapi.repository.RoleRepository;
import com.sm.jeyz9.storemateapi.repository.UserRepository;
import com.sm.jeyz9.storemateapi.services.UserManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserManagementServiceImpl implements UserManagementService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Autowired
    public UserManagementServiceImpl(UserRepository userRepository,
                                     RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    private void validateAdmin(User user) {
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getRoleName() == RoleName.ADMIN);
        if (!isAdmin) {
            throw new WebException(HttpStatus.FORBIDDEN, "คุณไม่มีสิทธิ์เข้าถึงข้อมูลนี้");
        }
    }

    private UserManagementDTO mapToDTO(User user) {
        String role = user.getRoles().stream()
                .map(r -> r.getRoleName().name())
                .findFirst()
                .orElse(null);

        return UserManagementDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(role)
                .isSuspended(user.isSuspended())
                .suspendAt(user.getSuspendAt())
                .build();
    }

    @Override
    public PaginationDTO<UserManagementDTO> getAllUsers(int page, int size, String email) {
        try {
            User currentUser = userRepository.findUserByEmail(email)
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบผู้ใช้งานในระบบ"));

            validateAdmin(currentUser);

            Pageable pageable = PageRequest.of(page, size);
            Page<User> users = userRepository.findAll(pageable);

            List<UserManagementDTO> data = users.getContent().stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());

            return new PaginationDTO<>(
                    data,
                    page,
                    size,
                    (int) users.getTotalElements()
            );

        } catch (WebException e) {
            throw e;
        } catch (Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "เกิดข้อผิดพลาดภายในเซิร์ฟเวอร์: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public UserManagementDTO updateUserRole(Long userId, UserRoleRequestDTO request, String email) {
        try {
            User currentUser = userRepository.findUserByEmail(email)
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบผู้ใช้งานในระบบ"));

            validateAdmin(currentUser);

            User targetUser = userRepository.findById(userId)
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบผู้ใช้งานที่ระบุ"));

            Role role = roleRepository.findByRoleName(request.getRoleName())
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบ Role ที่ระบุ"));

            targetUser.getRoles().clear();
            targetUser.getRoles().add(role);
            userRepository.save(targetUser);

            return mapToDTO(targetUser);

        } catch (WebException e) {
            throw e;
        } catch (Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "เกิดข้อผิดพลาดภายในเซิร์ฟเวอร์: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public UserManagementDTO suspendUser(Long userId, String email) {
        try {
            User currentUser = userRepository.findUserByEmail(email)
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบผู้ใช้งานในระบบ"));

            validateAdmin(currentUser);

            User targetUser = userRepository.findById(userId)
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบผู้ใช้งานที่ระบุ"));

            if (targetUser.isSuspended()) {
                throw new WebException(HttpStatus.BAD_REQUEST, "บัญชีนี้ถูกระงับอยู่แล้ว");
            }

            targetUser.setSuspended(true);
            targetUser.setSuspendAt(LocalDateTime.now());
            userRepository.save(targetUser);

            return mapToDTO(targetUser);

        } catch (WebException e) {
            throw e;
        } catch (Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "เกิดข้อผิดพลาดภายในเซิร์ฟเวอร์: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public UserManagementDTO activateUser(Long userId, String email) {
        try {
            User currentUser = userRepository.findUserByEmail(email)
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบผู้ใช้งานในระบบ"));

            validateAdmin(currentUser);

            User targetUser = userRepository.findById(userId)
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบผู้ใช้งานที่ระบุ"));

            if (!targetUser.isSuspended()) {
                throw new WebException(HttpStatus.BAD_REQUEST, "บัญชีนี้ใช้งานได้อยู่แล้ว");
            }

            targetUser.setSuspended(false);
            targetUser.setSuspendAt(null);
            userRepository.save(targetUser);

            return mapToDTO(targetUser);

        } catch (WebException e) {
            throw e;
        } catch (Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "เกิดข้อผิดพลาดภายในเซิร์ฟเวอร์: " + e.getMessage());
        }
    }
}