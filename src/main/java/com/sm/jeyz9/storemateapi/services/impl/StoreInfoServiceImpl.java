package com.sm.jeyz9.storemateapi.services.impl;

import com.sm.jeyz9.storemateapi.dto.StoreInfoDTO;
import com.sm.jeyz9.storemateapi.dto.StoreInfoRequestDTO;
import com.sm.jeyz9.storemateapi.exceptions.WebException;
import com.sm.jeyz9.storemateapi.models.RoleName;
import com.sm.jeyz9.storemateapi.models.StoreInfo;
import com.sm.jeyz9.storemateapi.models.User;
import com.sm.jeyz9.storemateapi.models.Zipcode;
import com.sm.jeyz9.storemateapi.repository.StoreInfoRepository;
import com.sm.jeyz9.storemateapi.repository.UserRepository;
import com.sm.jeyz9.storemateapi.services.StoreInfoService;
import com.sm.jeyz9.storemateapi.services.SupabaseService;
import com.sm.jeyz9.storemateapi.repository.ZipcodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StoreInfoServiceImpl implements StoreInfoService {

    private final StoreInfoRepository storeInfoRepository;
    private final UserRepository userRepository;
    private final SupabaseService supabaseService;
    private final ZipcodeRepository zipcodeRepository;

    @Autowired
    public StoreInfoServiceImpl(StoreInfoRepository storeInfoRepository,
                                UserRepository userRepository,
                                SupabaseService supabaseService,
                                ZipcodeRepository zipcodeRepository) {
        this.storeInfoRepository = storeInfoRepository;
        this.userRepository = userRepository;
        this.supabaseService = supabaseService;
        this.zipcodeRepository = zipcodeRepository;
    }

    private void validateAdmin(User user) {
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getRoleName() == RoleName.ADMIN);
        if (!isAdmin) {
            throw new WebException(HttpStatus.FORBIDDEN, "คุณไม่มีสิทธิ์เข้าถึงข้อมูลนี้");
        }
    }

    private StoreInfoDTO mapToStoreInfoDTO(StoreInfo storeInfo) {
        StoreInfoDTO dto = StoreInfoDTO.builder()
                .id(storeInfo.getId())
                .storeName(storeInfo.getStoreName())
                .phone(storeInfo.getPhone())
                .streetAddress(storeInfo.getStreetAddress())
                .email(storeInfo.getEmail())
                .promotionImage(storeInfo.getPromotionImage())
                .build();

        if (storeInfo.getZipcode() != null) {
            Zipcode zipcode = storeInfo.getZipcode();
            dto.setZipcode(zipcode.getZipcode());
            if (zipcode.getSubdistrict() != null) dto.setSubdistrict(zipcode.getSubdistrict().getName());
            if (zipcode.getDistrict() != null) dto.setDistrict(zipcode.getDistrict().getName());
            if (zipcode.getProvince() != null) dto.setProvince(zipcode.getProvince().getName());
        }

        return dto;
    }

    @Override
    public StoreInfoDTO getStoreDetails() {
        try {

            StoreInfo storeInfo = storeInfoRepository.findAll()
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบข้อมูลร้านค้า"));



            return mapToStoreInfoDTO(storeInfo);

        } catch (WebException e) {
            throw e;
        } catch (Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "เกิดข้อผิดพลาดภายในเซิร์ฟเวอร์: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public StoreInfoDTO updateStoreDetails(Long storeId, StoreInfoRequestDTO request, String email, MultipartFile image) {
        try {
            User owner = userRepository.findUserByEmail(email)
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบผู้ใช้งานในระบบ"));

            validateAdmin(owner);

            StoreInfo storeInfo = storeInfoRepository.findById(storeId)
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบข้อมูลร้านค้า หรือคุณไม่มีสิทธิ์แก้ไข"));

            if (request.getStoreName() != null && !request.getStoreName().isBlank()) {
                storeInfo.setStoreName(request.getStoreName());
            }
            if (request.getPhone() != null) {
                storeInfo.setPhone(request.getPhone());
            }
            if (request.getStreetAddress() != null) {
                storeInfo.setStreetAddress(request.getStreetAddress());
            }
            if (request.getZipcodeId() != null) {
                Zipcode zipcode = zipcodeRepository.findById(request.getZipcodeId())
                        .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบรหัสไปรษณีย์"));
                storeInfo.setZipcode(zipcode);
            }

            if (request.getEmail() != null) {
                storeInfo.setEmail(request.getEmail());
            }

            // จัดการ image
            if (image != null && !image.isEmpty()) {
                if (storeInfo.getPromotionImage() != null) {
                    supabaseService.deleteStoreImage(storeInfo.getPromotionImage());
                }
                String imageUrl = supabaseService.uploadStoreImage(image);
                storeInfo.setPromotionImage(imageUrl);
            }

            StoreInfo saved = storeInfoRepository.save(storeInfo);
            return mapToStoreInfoDTO(saved);

        } catch (WebException e) {
            throw e;
        } catch (Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "เกิดข้อผิดพลาดภายในเซิร์ฟเวอร์: " + e.getMessage());
        }
    }
}