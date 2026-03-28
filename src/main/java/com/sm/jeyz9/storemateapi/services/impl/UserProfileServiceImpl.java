package com.sm.jeyz9.storemateapi.services.impl;

import com.sm.jeyz9.storemateapi.dto.UserAddressDTO;
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
import java.util.List;

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
            return userRepository.save(user);

        } catch (WebException e) {
            throw e;
        } catch (Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "เกิดข้อผิดพลาดภายในเซิร์ฟเวอร์: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public UserAddressDTO addUserAddress(String email, UserAddressRequestDTO dto) {
        try {
            User user = userRepository.findUserByEmail(email)
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบผู้ใช้งาน"));

            Subdistrict subdistrict = subdistrictRepository.findById(dto.getSubdistrictId())
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบข้อมูลตำบล/แขวงที่ระบุ"));

            if (Boolean.TRUE.equals(dto.getIsDefault())) {
                userAddressRepository.resetDefaultAddress(user.getId());
            }

            UserAddress address = UserAddress.builder()
                    .user(user)
                    .streetAddress(dto.getStreetAddress())
                    .subdistrict(subdistrict)
                    .isDefault(dto.getIsDefault() != null && dto.getIsDefault())
                    .createdAt(LocalDateTime.now())
                    .build();

            UserAddress savedAddress = userAddressRepository.save(address);

            return UserAddressDTO.builder()
                    .id(savedAddress.getId())
                    .receiverName(user.getName()) // ชื่อจาก User
                    .receiverPhone(user.getPhone()) // เบอร์จาก User
                    .fullAddress(String.format("%s ต.%s อ.%s จ.%s %s",
                            savedAddress.getStreetAddress(),
                            subdistrict.getName(), // ชื่อตำบล
                            subdistrict.getDistrict().getName(), // ชื่ออำเภอ
                            subdistrict.getDistrict().getProvince().getName(), // ชื่อจังหวัด
                            subdistrict.getPostal_code())) // รหัสไปรษณีย์
                    .isDefault(savedAddress.getIsDefault())
                    .build();

        } catch (WebException e) {
            throw e;
        } catch (Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "เกิดข้อผิดพลาดภายในเซิร์ฟเวอร์: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserAddressDTO> getUserAddresses(String email) {
        try {
            User user = userRepository.findUserByEmail(email)
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบผู้ใช้งาน"));

            List<UserAddress> addresses = userAddressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(user.getId());

            return addresses.stream().map(addr -> {
                // ดึงข้อมูลส่วนต่างๆ มาประกอบเป็นที่อยู่เต็ม
                String subdistrict = addr.getSubdistrict().getName();
                String district = addr.getSubdistrict().getDistrict().getName();
                String province = addr.getSubdistrict().getDistrict().getProvince().getName();
                String postalCode = addr.getSubdistrict().getPostal_code();

                // จัดรูปแบบ: "116/1 ม.1 ต.ห้วยขวาง อ.กำแพงแสน จ.นครปฐม 73140"
                String formattedAddress = String.format("%s ต.%s อ.%s จ.%s %s",
                        addr.getStreetAddress(), subdistrict, district, province, postalCode);

                return UserAddressDTO.builder()
                        .id(addr.getId())
                        .receiverName(user.getName())
                        .receiverPhone(user.getPhone())
                        .fullAddress(formattedAddress)
                        .isDefault(addr.getIsDefault())
                        .build();
            }).toList();

        } catch (WebException e) {
            throw e;
        } catch (Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "เกิดข้อผิดพลาดภายในเซิร์ฟเวอร์: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserAddressDTO getUserAddressById(Long addressId, String email) {
        try {
            UserAddress addr = userAddressRepository.findById(addressId)
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบข้อมูลที่อยู่ที่ระบุ"));

            // 2. ตรวจสอบว่าที่อยู่นี้เป็นของผู้ใช้ที่ Login อยู่จริงหรือไม่ (Security Check)
            if (!addr.getUser().getEmail().equals(email)) {
                throw new WebException(HttpStatus.FORBIDDEN, "คุณไม่มีสิทธิ์เข้าถึงข้อมูลที่อยู่นี้");
            }

            return UserAddressDTO.builder()
                    .id(addr.getId())
                    .receiverName(addr.getUser().getName())
                    .receiverPhone(addr.getUser().getPhone())
                    .fullAddress(String.format("%s ต.%s อ.%s จ.%s %s",
                            addr.getStreetAddress(),
                            addr.getSubdistrict().getName(),
                            addr.getSubdistrict().getDistrict().getName(),
                            addr.getSubdistrict().getDistrict().getProvince().getName(),
                            addr.getSubdistrict().getPostal_code()))
                    .isDefault(addr.getIsDefault())
                    .build();
        } catch (WebException e) {
            throw e;
        } catch (Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "เกิดข้อผิดพลาดภายในเซิร์ฟเวอร์: " + e.getMessage());
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

    
}