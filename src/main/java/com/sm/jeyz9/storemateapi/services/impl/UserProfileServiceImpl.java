package com.sm.jeyz9.storemateapi.services.impl;

import com.sm.jeyz9.storemateapi.dto.UserAddressDTO;
import com.sm.jeyz9.storemateapi.dto.UserAddressRequestDTO;
import com.sm.jeyz9.storemateapi.dto.UserProfileRequestDTO;
import com.sm.jeyz9.storemateapi.exceptions.WebException;
import com.sm.jeyz9.storemateapi.models.District;
import com.sm.jeyz9.storemateapi.models.Province;
import com.sm.jeyz9.storemateapi.models.Subdistrict;
import com.sm.jeyz9.storemateapi.models.User;
import com.sm.jeyz9.storemateapi.models.UserAddress;
import com.sm.jeyz9.storemateapi.models.Zipcode;
import com.sm.jeyz9.storemateapi.repository.SubdistrictRepository;
import com.sm.jeyz9.storemateapi.repository.UserAddressRepository;
import com.sm.jeyz9.storemateapi.repository.UserRepository;
import com.sm.jeyz9.storemateapi.repository.ZipcodeRepository;
import com.sm.jeyz9.storemateapi.services.UserProfileService;
import com.sm.jeyz9.storemateapi.services.SupabaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserRepository userRepository;
    private final SupabaseService supabaseService;
    private final UserAddressRepository userAddressRepository;
    private final ZipcodeRepository zipcodeRepository;

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
    public UserProfileRequestDTO updateProfile(String email, UserProfileRequestDTO dto, MultipartFile image) {
        try {
            // 1. ดึงข้อมูล User เดิม
            User user = userRepository.findUserByEmail(email)
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบผู้ใช้งาน"));

            // 2. จัดการเรื่องรูปภาพ (ถ้ามีการส่งมา)
            if (image != null && !image.isEmpty()) {
                String imageUrl = supabaseService.uploadUserAvatar(image);
                user.setImageUrl(imageUrl);
            }

            // 3. อัปเดตข้อมูลพื้นฐาน
            if (dto != null) {
                if (dto.getName() != null && !dto.getName().trim().isEmpty()) {
                    user.setName(dto.getName());
                }

                // เช็คอีเมลซ้ำ (ถ้ามีการเปลี่ยน)
                if (dto.getEmail() != null && !dto.getEmail().equalsIgnoreCase(user.getEmail())) {
                    if (userRepository.existsUserByEmail(dto.getEmail())) {
                        throw new WebException(HttpStatus.BAD_REQUEST, "อีเมลนี้มีผู้อื่นใช้งานแล้ว");
                    }
                    user.setEmail(dto.getEmail());
                }

                if (dto.getPhone() != null && !dto.getPhone().equals(user.getPhone())) {
                    if (userRepository.existsUserByPhone(dto.getPhone())) {
                        throw new WebException(HttpStatus.BAD_REQUEST, "เบอร์โทรศัพท์นี้มีผู้อื่นใช้งานแล้ว");
                    }
                    user.setPhone(dto.getPhone());
                }
            }

            // 4. บันทึกลง Database
            User updatedUser = userRepository.save(user);

            // 5. Return กลับเป็น DTO (ไม่ส่ง Model ตรงๆ)
            return UserProfileRequestDTO.builder()
                    .name(updatedUser.getName())
                    .email(updatedUser.getEmail())
                    .phone(updatedUser.getPhone())
                    .imageUrl(updatedUser.getImageUrl())
                    .createdAt(updatedUser.getCreatedAt() != null ? updatedUser.getCreatedAt().toString() : null)
                    .build();

        } catch (WebException e) {
            throw e;
        } catch (Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "เกิดข้อผิดพลาดภายในเซิร์ฟเวอร์: " + e.getMessage());
        }
    }

    // Helper Method สำหรับใช้ร่วมกัน (Add/Update/Get)
    private UserAddressDTO mapToDTO(UserAddress addr, User user) {
        Zipcode z = addr.getZipcode();
        if (z == null) return null;

        Subdistrict sub = z.getSubdistrict();
        District dist = z.getDistrict();
        Province prov = z.getProvince();

        String fullAddress = String.format("%s ต.%s อ.%s จ.%s %s",
                addr.getStreetAddress(),
                sub.getName(),
                dist.getName(),
                prov.getName(),
                z.getZipcode());

        return UserAddressDTO.builder()
                .id(addr.getId())
                .receiverName(user.getName())
                .receiverPhone(user.getPhone())
                .fullAddress(fullAddress)
                .isDefault(addr.getIsDefault())
                .build();
    }


    @Override
    @Transactional
    public UserAddressDTO addUserAddress(String email, UserAddressRequestDTO dto) {
        try{
            User user = userRepository.findUserByEmail(email)
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบผู้ใช้งาน"));

            Zipcode z = zipcodeRepository.findById(dto.getZipcodeId())
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบรหัสไปรษณีย์"));

            // บังคับ Reset อันเก่าให้เป็น false
            userAddressRepository.resetDefaultAddress(user.getId());

            UserAddress address = UserAddress.builder()
                    .user(user)
                    .streetAddress(dto.getStreetAddress())
                    .zipcode(z) // เซต Zipcode Entity ลงไปตรงๆ ตาม Model ใหม่
                    .isDefault(true)
                    .createdAt(LocalDateTime.now())
                    .build();

            UserAddress saved = userAddressRepository.save(address);
            return mapToDTO(saved, user);
        } catch (WebException e) {
        throw e;
    } catch (Exception e) {
        throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "เกิดข้อผิดพลาด: " + e.getMessage());
    }
}
    
    
    @Override
    public List<UserAddressDTO> getUserAddresses(String email) {
        try{
            User user = userRepository.findUserByEmail(email)
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบผู้ใช้งาน"));

            // ดึงที่อยู่ทั้งหมด และ map เป็น DTO (ข้อมูลตำบล/รหัสไปรษณีย์อยู่ใน addr.getZipcode() แล้ว)
            return userAddressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(user.getId()).stream()
                    .map(addr -> mapToDTO(addr, user))
                    .filter(dto -> dto != null)
                    .collect(Collectors.toList());
        } catch (WebException e) {
        throw e;
    } catch (Exception e) {
        throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "เกิดข้อผิดพลาด: " + e.getMessage());
    }
}

    @Override
    public UserAddressDTO getUserAddressById(Long id, String email) {
        try {
            User user = userRepository.findUserByEmail(email)
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบผู้ใช้งาน"));

            UserAddress addr = userAddressRepository.findById(id)
                    .filter(a -> a.getUser().getId().equals(user.getId()))
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบที่อยู่"));

            return mapToDTO(addr, user);
        } catch (WebException e) {
        throw e;
    } catch (Exception e) {
        throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "เกิดข้อผิดพลาด: " + e.getMessage());
    }
}
    
    
    @Override
    @Transactional
    public void deleteUserAddress(Long addressId, String email) {
        try {
            UserAddress addressToDelete = userAddressRepository.findById(addressId)
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบข้อมูลที่อยู่ที่ระบุ"));

            if (!addressToDelete.getUser().getEmail().equals(email)) {
                throw new WebException(HttpStatus.FORBIDDEN, "คุณไม่มีสิทธิ์ลบที่อยู่นี้");
            }

            boolean wasDefault = Boolean.TRUE.equals(addressToDelete.getIsDefault());

            userAddressRepository.delete(addressToDelete);

            if (wasDefault) {
                List<UserAddress> remainingAddresses = userAddressRepository
                        .findByUserIdOrderByIsDefaultDescCreatedAtDesc(addressToDelete.getUser().getId());

                if (!remainingAddresses.isEmpty()) {
                    UserAddress newDefault = remainingAddresses.getFirst();
                    newDefault.setIsDefault(true);
                    userAddressRepository.save(newDefault);
                }
            }
        } catch (WebException e) {
            throw e;
        } catch (Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "เกิดข้อผิดพลาดภายในเซิร์ฟเวอร์: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public UserAddressDTO updateUserAddress(Long id, UserAddressRequestDTO dto, String email) {
        try {
            User user = userRepository.findUserByEmail(email)
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบผู้ใช้งาน"));

            UserAddress addr = userAddressRepository.findById(id)
                    .filter(a -> a.getUser().getId().equals(user.getId()))
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบที่อยู่"));

            // บังคับเป็น Default
            userAddressRepository.resetDefaultAddress(user.getId());
            addr.setIsDefault(true);

            if (dto.getZipcodeId() != null) {
                Zipcode z = zipcodeRepository.findById(dto.getZipcodeId())
                        .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบรหัสไปรษณีย์"));
                addr.setZipcode(z); // เปลี่ยน Zipcode ใหม่
            }

            if (dto.getStreetAddress() != null) {
                addr.setStreetAddress(dto.getStreetAddress());
            }

            return mapToDTO(userAddressRepository.save(addr), user);
        } catch (WebException e) {
            throw e;
        } catch (Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "เกิดข้อผิดพลาด: " + e.getMessage());
        }
    }


    @Override
    @Transactional
    public UserAddressDTO setDefaultAddress(Long addressId, String email) {
        try {
            User user = userRepository.findUserByEmail(email)
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบผู้ใช้งาน"));

            UserAddress addr = userAddressRepository.findByUserIdAndIsDefaultTrue(user.getId())
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ยังไม่มีที่อยู่เริ่มต้น"));

            return mapToDTO(addr, user);
        } catch (WebException e) {
            throw e;
        } catch (Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "เกิดข้อผิดพลาด: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public UserAddressDTO getDefaultAddress(String email) {
        try {
        User user = userRepository.findUserByEmail(email)
                .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบผู้ใช้งาน"));

        UserAddress addr = userAddressRepository.findByUserIdAndIsDefaultTrue(user.getId())
                .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ยังไม่มีที่อยู่เริ่มต้น"));

        return mapToDTO(addr, user);
        } catch (WebException e) {
            throw e;
        } catch (Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "เกิดข้อผิดพลาด: " + e.getMessage());
        }
    }
    
}