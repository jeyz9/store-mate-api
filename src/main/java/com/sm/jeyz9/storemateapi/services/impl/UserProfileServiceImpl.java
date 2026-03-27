package com.sm.jeyz9.storemateapi.services.impl;

import com.sm.jeyz9.storemateapi.dto.UserAddressRequestDTO;
import com.sm.jeyz9.storemateapi.dto.UserProfileRequestDTO;
import com.sm.jeyz9.storemateapi.exceptions.WebException;
import com.sm.jeyz9.storemateapi.models.Subdistrict;
import com.sm.jeyz9.storemateapi.models.User;
import com.sm.jeyz9.storemateapi.models.UserAddress;
import com.sm.jeyz9.storemateapi.repository.SubdistrictRepository;
import com.sm.jeyz9.storemateapi.repository.UserAddressRepository;
import com.sm.jeyz9.storemateapi.repository.UserRepository;
import com.sm.jeyz9.storemateapi.services.UserProfileService;
import com.sm.jeyz9.storemateapi.services.SupabaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserRepository userRepository;
    private final SupabaseService supabaseService;
    private final UserAddressRepository userAddressRepository;
    private final SubdistrictRepository subdistrictRepository;

    @Override
    public UserProfileRequestDTO getUserProfile(String email) {
        try {
        User user = userRepository.findUserByEmail(email)
                .orElseThrow(() -> new RuntimeException("ไม่พบผู้ใช้งานในระบบ"));

        return UserProfileRequestDTO.builder()
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .imageUrl(user.getImageUrl())
                //ในกรณที่ user ไม่มี createdAt เส้นนี้จะไม่พัง
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                .createdAt(String.valueOf(user.getCreatedAt()))
                .build();
        } catch (WebException e) {
            throw e;
        } catch (Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "เกิดข้อผิดพลาดภายในเซิร์ฟเวอร์: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public User updateProfile(String email, UserProfileRequestDTO dto, MultipartFile image) {
        try {
            // 1. ดึงข้อมูลผู้ใช้ปัจจุบันออกมาจากฐานข้อมูลก่อน
            User user = userRepository.findUserByEmail(email)
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบผู้ใช้งาน"));

            if (image != null && !image.isEmpty()) {
                String imageUrl = supabaseService.uploadUserAvatar(image);
                user.setImageUrl(imageUrl);
            }

            if (dto != null) {
                if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
                    String newEmail = dto.getEmail().trim();
                    // เช็คว่า "ถ้าเมลใหม่ไม่ตรงกับเมลเดิม" ถึงจะไปเช็คว่าซ้ำกับคนอื่นไหม
                    if (!newEmail.equalsIgnoreCase(user.getEmail())) {
                        if (userRepository.existsUserByEmail(newEmail)) {
                            throw new WebException(HttpStatus.BAD_REQUEST, "อีเมลนี้มีผู้อื่นใช้งานแล้ว");
                        }
                        user.setEmail(newEmail);
                    }
                }

                // --- เช็คและอัปเดตเบอร์โทรศัพท์ (Phone) ---
                if (dto.getPhone() != null && !dto.getPhone().trim().isEmpty()) {
                    String newPhone = dto.getPhone().trim();
                    // เช็คว่า ถ้าเบอร์ใหม่ไม่ตรงกับเบอร์เดิม
                    if (!newPhone.equals(user.getPhone())) {
                        if (userRepository.existsUserByPhone(newPhone)) {
                            throw new WebException(HttpStatus.BAD_REQUEST, "เบอร์โทรศัพท์นี้มีผู้อื่นใช้งานแล้ว");
                        }
                        user.setPhone(newPhone);
                    }
                }
                if (dto.getName() != null && !dto.getName().trim().isEmpty()) {
                    user.setName(dto.getName());
                }
            }
            // 4. บันทึกและคืนค่า Object User (ตาม Return Type ของ Method)
            return userRepository.save(user);

        } catch (WebException e) {
            throw e;
        } catch (Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "เกิดข้อผิดพลาดภายในเซิร์ฟเวอร์: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public UserAddress addUserAddress(String email, UserAddressRequestDTO dto) {
        // 1. ดึงข้อมูลเจ้าของบัญชี
        User user = userRepository.findUserByEmail(email)
                .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบผู้ใช้งาน"));

        // 2. ดึงข้อมูลตำบล (Subdistrict) เพื่อเชื่อมโยง District และ Province
        Subdistrict subdistrict = subdistrictRepository.findById(dto.getSubdistrictId())
                .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบข้อมูลตำบล/แขวงที่ระบุ"));

        // 3. จัดการเรื่องที่อยู่เริ่มต้น (Default Address)
        if (Boolean.TRUE.equals(dto.getIsDefault())) {
            // ให้ที่อยู่อื่นๆ ของ User คนนี้ไม่เป็น Default ก่อน
            userAddressRepository.resetDefaultAddress(user.getId());
        }

        // 4. สร้าง Entity UserAddress
        UserAddress address = UserAddress.builder()
                .streetAddress(dto.getStreetAddress())
                .subdistrict(subdistrict)
                .isDefault(dto.getIsDefault() != null && dto.getIsDefault())
                .createdAt(LocalDateTime.now())
                .build();

        return userAddressRepository.save(address);
    }
}