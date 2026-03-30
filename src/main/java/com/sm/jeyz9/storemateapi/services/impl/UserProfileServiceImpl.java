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
        try{
        User user = userRepository.findUserByEmail(email)
                .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบผู้ใช้งาน"));

        Zipcode zipcodeEntity = zipcodeRepository.findById(dto.getZipcodeId())
                .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบข้อมูลรหัสไปรษณีย์"));

   
        if (dto.getIsDefault() != null && dto.getIsDefault()) {
            userAddressRepository.resetDefaultAddress(user.getId());
        }

        UserAddress address = UserAddress.builder()
                .user(user)
                .streetAddress(dto.getStreetAddress())
                .subdistrict(zipcodeEntity.getSubdistrict())
                .isDefault(true)
                .createdAt(LocalDateTime.now())
                .build();

        UserAddress saved = userAddressRepository.save(address);

        return mapToDTOWithZipcode(saved, user, zipcodeEntity.getZipcode());
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

        // ดึงที่อยู่ทั้งหมดของ User นี้
        List<UserAddress> addresses = userAddressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(user.getId());

        // แปลงแต่ละ Address เป็น DTO
        return addresses.stream()
                .map(addr -> {
                    // หา zipcode จาก zipcodeRepository โดยใช้ subdistrict id
                    String zipcode = zipcodeRepository.findBySubdistrictId(addr.getSubdistrict().getId())
                            .map(Zipcode::getZipcode)
                            .orElse("");
                    return mapToDTOWithZipcode(addr, user, zipcode);
                })
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

        // หาที่อยู่ตาม ID และต้องเป็นของ User คนนี้ด้วยเพื่อความปลอดภัย
        UserAddress address = userAddressRepository.findById(id)
                .filter(addr -> addr.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบข้อมูลที่อยู่ หรือคุณไม่มีสิทธิ์เข้าถึง"));

        // หา zipcode มาประกอบร่าง
        String zipcode = zipcodeRepository.findBySubdistrictId(address.getSubdistrict().getId())
                .map(Zipcode::getZipcode)
                .orElse("");

        return mapToDTOWithZipcode(address, user, zipcode);
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

        UserAddress address = userAddressRepository.findById(id)
                .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบข้อมูลที่อยู่"));

        if (!address.getUser().getId().equals(user.getId())) {
            throw new WebException(HttpStatus.FORBIDDEN, "คุณไม่มีสิทธิ์แก้ไขที่อยู่นี้");
        }

        userAddressRepository.resetDefaultAddress(user.getId());
        address.setIsDefault(true); 

        String currentZipcode = "";
        if (dto.getZipcodeId() != null) {
            Zipcode zipcodeEntity = zipcodeRepository.findById(dto.getZipcodeId())
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบข้อมูลรหัสไปรษณีย์"));
            address.setSubdistrict(zipcodeEntity.getSubdistrict());
            currentZipcode = zipcodeEntity.getZipcode();
        } else {
            currentZipcode = zipcodeRepository.findBySubdistrictId(address.getSubdistrict().getId())
                    .map(Zipcode::getZipcode)
                    .orElse("");
        }

        if (dto.getStreetAddress() != null) {
            address.setStreetAddress(dto.getStreetAddress());
        }

        UserAddress saved = userAddressRepository.save(address);
        return mapToDTOWithZipcode(saved, user, currentZipcode);
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

            UserAddress address = userAddressRepository.findById(addressId)
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบข้อมูลที่อยู่ที่ระบุ"));

            if (!address.getUser().getId().equals(user.getId())) {
                throw new WebException(HttpStatus.FORBIDDEN, "คุณไม่มีสิทธิ์แก้ไขที่อยู่นี้");
            }

            userAddressRepository.resetDefaultAddress(user.getId());

            address.setIsDefault(true);
            UserAddress updatedAddress = userAddressRepository.save(address);

            String zipcode = zipcodeRepository.findBySubdistrictId(updatedAddress.getSubdistrict().getId())
                    .map(Zipcode::getZipcode)
                    .orElse("");

            return mapToDTOWithZipcode(updatedAddress, user, zipcode);

        } catch (WebException e) {
            throw e;
        } catch (Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "เกิดข้อผิดพลาด: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public UserAddressDTO getDefaultAddress(String email) {
        // 1. หา User จาก email
        User user = userRepository.findUserByEmail(email)
                .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบผู้ใช้งาน"));

        // 2. ค้นหาที่อยู่ที่ถูกตั้งเป็น Default (isDefault = true)
        UserAddress defaultAddress = userAddressRepository.findByUserIdAndIsDefaultTrue(user.getId())
                .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "คุณยังไม่ได้ตั้งค่าที่อยู่เริ่มต้น"));

        // 3. ดึงรหัสไปรษณีย์ผ่าน ZipcodeRepository
        String zipcode = zipcodeRepository.findBySubdistrictId(defaultAddress.getSubdistrict().getId())
                .map(Zipcode::getZipcode)
                .orElse("");

        // 4. ส่งกลับเป็น DTO โดยใช้ Helper Method ตัวเดิม
        return mapToDTOWithZipcode(defaultAddress, user, zipcode);
    }
    
}