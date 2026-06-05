package com.sm.jeyz9.storemateapi.services.impl;

import com.sm.jeyz9.storemateapi.dto.StoreInfoDTO;
import com.sm.jeyz9.storemateapi.dto.StoreInfoRequestDTO;
import com.sm.jeyz9.storemateapi.exceptions.WebException;
import com.sm.jeyz9.storemateapi.models.RoleName;
import com.sm.jeyz9.storemateapi.models.StoreInfo;
import com.sm.jeyz9.storemateapi.models.User;
import com.sm.jeyz9.storemateapi.repository.StoreInfoRepository;
import com.sm.jeyz9.storemateapi.repository.UserRepository;
import com.sm.jeyz9.storemateapi.services.StoreInfoService;
import com.sm.jeyz9.storemateapi.services.SupabaseService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StoreInfoServiceImpl implements StoreInfoService {

    private final StoreInfoRepository storeInfoRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final SupabaseService supabaseService;

    @Autowired
    public StoreInfoServiceImpl(StoreInfoRepository storeInfoRepository,
                                UserRepository userRepository,
                                ModelMapper modelMapper,
                                SupabaseService supabaseService) {
        this.storeInfoRepository = storeInfoRepository;
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
        this.supabaseService = supabaseService;
    }

    private void validateAdmin(User user) {
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getRoleName() == RoleName.ADMIN);
        if (!isAdmin) {
            throw new WebException(HttpStatus.FORBIDDEN, "คุณไม่มีสิทธิ์เข้าถึงข้อมูลนี้");
        }
    }

    @Override
    public StoreInfoDTO getStoreDetails() {
        try {

            StoreInfo storeInfo = storeInfoRepository.findAll()
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "ไม่พบข้อมูลร้านค้า"));

            return modelMapper.map(storeInfo, StoreInfoDTO.class);

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
            return modelMapper.map(saved, StoreInfoDTO.class);

        } catch (WebException e) {
            throw e;
        } catch (Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "เกิดข้อผิดพลาดภายในเซิร์ฟเวอร์: " + e.getMessage());
        }
    }
}