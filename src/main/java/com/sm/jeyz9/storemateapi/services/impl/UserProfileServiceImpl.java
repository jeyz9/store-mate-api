package com.sm.jeyz9.storemateapi.services.impl;

import com.sm.jeyz9.storemateapi.dto.UserProfileRequestDTO;
import com.sm.jeyz9.storemateapi.models.User;
import com.sm.jeyz9.storemateapi.repository.UserRepository;
import com.sm.jeyz9.storemateapi.services.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserRepository userRepository;

    @Override
    public UserProfileRequestDTO getUserProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("ไม่พบผู้ใช้งานในระบบ"));

        return UserProfileRequestDTO.builder()
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .imageUrl(user.getImageUrl())
                .createdAt(user.getCreatedAt().toString()) // เปลี่ยนให้ตรงกับชื่อฟิลด์ใน DTO
                .build();
    }


    @Override
    @Transactional
    public User updateProfile(Long userId, UserProfileRequestDTO dto) {
        // แก้ไข: ใช้ userId ในการค้นหาแทน email ที่ไม่มีอยู่ในพารามิเตอร์
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("ไม่พบผู้ใช้งานในระบบ"));

        // ส่วนการ Update ข้อมูล (เหมือนเดิม)
        if (dto.getName() != null) user.setName(dto.getName());
        if (dto.getPhone() != null) user.setPhone(dto.getPhone());
        if (dto.getImageUrl() != null) user.setImageUrl(dto.getImageUrl());

        return userRepository.save(user);
    }
}