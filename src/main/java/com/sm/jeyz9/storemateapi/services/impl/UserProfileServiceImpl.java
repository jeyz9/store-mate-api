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

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserRepository userRepository;
    private final SupabaseService supabaseService;
    private final UserAddressRepository userAddressRepository;
    private final SubdistrictRepository subdistrictRepository;
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

    // Helper Method สำหรับใช้ร่วมกัน (Add/Update/Get)
    private UserAddressDTO mapToDTOWithZipcode(UserAddress addr, User user, String zipcode) {
        Subdistrict sub = addr.getSubdistrict();
        District dist = sub.getDistrict();
        Province prov = dist.getProvince();

        // ประกอบ Full Address โดยใช้ชื่อตัวแปร zipcode ให้สอดคล้องกับ Model
        String fullAddress = String.format("%s ต.%s อ.%s จ.%s %s",
                addr.getStreetAddress(),
                sub.getName(),
                dist.getName(),
                prov.getName(),
                zipcode);

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
        User user = userRepository.findUserByEmail(email)
                .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบผู้ใช้งาน"));

        Zipcode zipcodeEntity = zipcodeRepository.findById(dto.getZipcodeId())
                .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบข้อมูลรหัสไปรษณีย์"));

        UserAddress address = UserAddress.builder()
                .user(user)
                .streetAddress(dto.getStreetAddress())
                .subdistrict(zipcodeEntity.getSubdistrict())
                .isDefault(dto.getIsDefault() != null && dto.getIsDefault())
                .createdAt(LocalDateTime.now())
                .build();

        UserAddress saved = userAddressRepository.save(address);

        return mapToDTOWithZipcode(saved, user, zipcodeEntity.getZipcode());
    }
/*
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

*/
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
/*
    @Override
    @Transactional
    public UserAddressDTO updateUserAddress(Long addressId, UserAddressRequestDTO dto, String email) {
        try {
            // 1. ดึงข้อมูล User และที่อยู่เดิมที่ต้องการแก้ไข
            User user = userRepository.findUserByEmail(email)
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบผู้ใช้งาน"));

            UserAddress address = userAddressRepository.findById(addressId)
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบข้อมูลที่อยู่ที่ระบุ"));

            // 2. ตรวจสอบสิทธิ์ (Security Check): ต้องเป็นเจ้าของที่อยู่ถึงจะแก้ได้
            if (!address.getUser().getId().equals(user.getId())) {
                throw new WebException(HttpStatus.FORBIDDEN, "คุณไม่มีสิทธิ์แก้ไขที่อยู่นี้");
            }

            // 3. อัปเดตข้อมูลตำบล (Subdistrict) หากมีการส่ง ID มาใหม่
            Subdistrict subdistrict = address.getSubdistrict();
            if (dto.getSubdistrictId() != null) {
                subdistrict = subdistrictRepository.findById(dto.getSubdistrictId())
                        .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบข้อมูลตำบล/แขวงที่ระบุ"));
                address.setSubdistrict(subdistrict);
            }

            // 4. จัดการเรื่องที่อยู่เริ่มต้น (Default Address) - อิงจาก logic add address
            if (Boolean.TRUE.equals(dto.getIsDefault())) {
                userAddressRepository.resetDefaultAddress(user.getId());
                address.setIsDefault(true);
            } else if (dto.getIsDefault() != null) {
                address.setIsDefault(false);
            }

            // 5. อัปเดตข้อมูลถนน/บ้านเลขที่
            if (dto.getStreetAddress() != null) {
                address.setStreetAddress(dto.getStreetAddress());
            }

            // 6. บันทึกการเปลี่ยนแปลง
            UserAddress updatedAddress = userAddressRepository.save(address);

            // 7. ส่งค่ากลับเป็น DTO ที่จัดรูปแบบแล้ว (เหมือนใน addUserAddress)
            return UserAddressDTO.builder()
                    .id(updatedAddress.getId())
                    .receiverName(user.getName())
                    .receiverPhone(user.getPhone())
                    .fullAddress(String.format("%s ต.%s อ.%s จ.%s %s",
                            updatedAddress.getStreetAddress(),
                            subdistrict.getName(),
                            subdistrict.getDistrict().getName(),
                            subdistrict.getDistrict().getProvince().getName(),
                            subdistrict.getPostal_code()))
                    .isDefault(updatedAddress.getIsDefault())
                    .build();

        } catch (WebException e) {
            throw e;
        } catch (Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "เกิดข้อผิดพลาดภายในเซิร์ฟเวอร์: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public UserAddressDTO setDefaultAddress(Long addressId, String email) {
        try {
            // 1. ดึงข้อมูล User และตรวจสอบว่ามีที่อยู่นี้อยู่จริงหรือไม่
            User user = userRepository.findUserByEmail(email)
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบผู้ใช้งาน"));

            UserAddress address = userAddressRepository.findById(addressId)
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบข้อมูลที่อยู่ที่ระบุ"));

            // 2. ตรวจสอบสิทธิ์ความเป็นเจ้าของ
            if (!address.getUser().getId().equals(user.getId())) {
                throw new WebException(HttpStatus.FORBIDDEN, "คุณไม่มีสิทธิ์แก้ไขที่อยู่นี้");
            }

            // 3. Reset ที่อยู่เดิมทั้งหมดของ User คนนี้ไม่ให้เป็น Default
            userAddressRepository.resetDefaultAddress(user.getId());

            // 4. ตั้งค่าที่อยู่นี้ให้เป็น Default
            address.setIsDefault(true);
            UserAddress updatedAddress = userAddressRepository.save(address);

            // 5. ส่งกลับเป็น DTO เพื่อแจ้ง Frontend ให้ Update UI
            return UserAddressDTO.builder()
                    .id(updatedAddress.getId())
                    .receiverName(user.getName())
                    .receiverPhone(user.getPhone())
                    .fullAddress(String.format("%s ต.%s อ.%s จ.%s %s",
                            updatedAddress.getStreetAddress(),
                            updatedAddress.getSubdistrict().getName(),
                            updatedAddress.getSubdistrict().getDistrict().getName(),
                            updatedAddress.getSubdistrict().getDistrict().getProvince().getName(),
                            updatedAddress.getSubdistrict().getPostal_code()))
                    .isDefault(true)
                    .build();

        } catch (WebException e) {
            throw e;
        } catch (Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "เกิดข้อผิดพลาด: " + e.getMessage());
        }
    }
 
    
 */
}