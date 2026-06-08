package com.sm.jeyz9.storemateapi.services;

import com.sm.jeyz9.storemateapi.exceptions.WebException;
import com.sm.jeyz9.storemateapi.models.PasswordResetToken;
import com.sm.jeyz9.storemateapi.models.User;
import com.sm.jeyz9.storemateapi.repository.PasswordResetTokenRepository;
import com.sm.jeyz9.storemateapi.repository.UserRepository;
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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

@Service
public class ThaibluksmsService {
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RestTemplate restTemplate;
    
    @Value("${thaibulksms.key}")
    private String thaibluksmsKey;
    
    @Value("${thaibulksms.secret}")
    private String thaibluksmsSecret;

    @Value("${thaibulksms.send_url}")
    private String sendEmailUrl;

    @Value("${thaibulksms.email_sender}")
    private String senderEmail;
    
    @Value("${thaibulksms.reset_password_template_uuid}")
    private String templateEmail;
    
    @Value("${front_end_url}")
    private String frontendUrl;

    @Autowired
    public ThaibluksmsService(UserRepository userRepository, PasswordResetTokenRepository passwordResetTokenRepository, RestTemplate restTemplate) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.restTemplate = restTemplate;
    }

    @Transactional
    public String sendEmailResetPassword(String email) {
        try{
            User user = userRepository.findUserByEmail(email).orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "User not found."));

            if(user.isSuspended()) {
                throw new WebException(HttpStatus.FORBIDDEN, "Account has been suspended");
            }
            
            String token = UUID.randomUUID().toString();
            PasswordResetToken reset = PasswordResetToken.builder()
                    .token(token)
                    .user(user)
                    .expiredAt(LocalDateTime.now().plusMinutes(15))
                    .createdAt(LocalDateTime.now())
                    .build();
            passwordResetTokenRepository.save(reset);

            HttpHeaders header = new HttpHeaders();
            header.setContentType(MediaType.APPLICATION_JSON);
            header.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            header.setBasicAuth(thaibluksmsKey, thaibluksmsSecret);

            String body = "{"
                    + "\"mail_from\":{"
                    + "\"email\":\"" + senderEmail + "\","
                    + "\"name\":\"Admin\""
                    + "},"
                    + "\"mail_to\":{"
                    + "\"email\":\"" + user.getEmail() + "\""
                    + "},"
                    + "\"payload\":{"
                    + "\"SERVER_URL\":\"" + frontendUrl + "\","
                    + "\"TOKEN\":\"" + token + "\""
                    + "},"
                    + "\"template_uuid\":\"" + templateEmail + "\","
                    + "\"subject\":\"รีเซ็ตรหัสผ่าน\""
                    + "}";

            HttpEntity<String> requestEntity = new HttpEntity<>(body, header);

            ResponseEntity<String> response = restTemplate.exchange(
                    sendEmailUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("รีเซ็ตรหัสผ่านไม่สำเร็จ: " + e.getMessage(), e);
        }
    }
}
