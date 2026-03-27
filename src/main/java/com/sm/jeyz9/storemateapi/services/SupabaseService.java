package com.sm.jeyz9.storemateapi.services;

import com.sm.jeyz9.storemateapi.exceptions.WebException;
import com.sm.jeyz9.storemateapi.models.Product;
import com.sm.jeyz9.storemateapi.models.ProductImage;
import com.sm.jeyz9.storemateapi.repository.ProductImageRepository;
import com.sm.jeyz9.storemateapi.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SupabaseService {
    private final ProductRepository productRepository;
    private final RestTemplate restTemplate;
    private final ProductImageRepository productImageRepository;

    @Value("${supabase.bucket.user:avatars}")
    private String userBucket;
    
    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    @Value("${supabase.bucket.product}")
    private String supabaseBucket;
    
    @Autowired
    public SupabaseService(ProductRepository productRepository, RestTemplate restTemplate, ProductImageRepository productImageRepository) {
        this.productRepository = productRepository;
        this.restTemplate = restTemplate;
        this.productImageRepository = productImageRepository;
    }
    
    @Transactional
    public void saveProductImages(Long productId, List<MultipartFile> files) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "Product not found."));
        if(files == null || files.isEmpty()) return;
        if(files.size() > 5) {
            throw new WebException(HttpStatus.BAD_REQUEST, "You can upload max 5 images.");
        }
        
        if(product.getProductImage() != null && product.getProductImage().size() >= 5) {
            throw new WebException(HttpStatus.BAD_REQUEST, "You can upload max 5 images.");
        }
        
        for(MultipartFile file : files) {
            try {
                String originalName = file.getOriginalFilename();
                String safeName = originalName.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");
                String fileName = System.currentTimeMillis() + "_" + safeName;

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                headers.set("apiKey", supabaseKey);
                headers.set("Authorization", "Bearer " + supabaseKey);

                HttpEntity<byte[]> entity = new HttpEntity<>(file.getBytes(), headers);
                
                String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString());
                String url = supabaseUrl + "/object/" + supabaseBucket + "/" + encodedFileName;
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
                
                if(!response.getStatusCode().is2xxSuccessful()) {
                    throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload image: " + fileName);
                }

                ProductImage productImage = ProductImage.builder()
                        .id(null)
                        .product(product)
                        .imageUrl(url)
                        .imageName(originalName)
                        .createdAt(LocalDateTime.now())
                        .build();
                productImageRepository.save(productImage);
            } catch (UnsupportedEncodingException e) {
                throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            } catch (IOException e) {
                throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            }
        }
    }

    @Transactional
    public String uploadUserAvatar(MultipartFile file) { // เอา static ออก
        if (file == null || file.isEmpty()) {
            throw new WebException(HttpStatus.BAD_REQUEST, "ไฟล์รูปภาพว่างเปล่า");
        }

        try {
            String originalName = file.getOriginalFilename();
            String safeName = originalName.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");
            String fileName = "profile_" + System.currentTimeMillis() + "_" + safeName;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.set("apiKey", supabaseKey);
            headers.set("Authorization", "Bearer " + supabaseKey);

            HttpEntity<byte[]> entity = new HttpEntity<>(file.getBytes(), headers);

            // ใช้ชื่อ Bucket สำหรับ User
            String url = supabaseUrl + "/storage/v1/object/" + userBucket + "/" + fileName;

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "ไม่สามารถอัปโหลดรูปโปรไฟล์ได้");
            }

            // คืนค่า Public URL กลับไป (ตรวจสอบโครงสร้าง URL ของ Supabase Project ของคุณอีกครั้ง)
            return supabaseUrl + "/storage/v1/object/public/" + userBucket + "/" + fileName;
        } catch (IOException e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "เกิดข้อผิดพลาดในการอ่านไฟล์: " + e.getMessage());
        }
    }
}
