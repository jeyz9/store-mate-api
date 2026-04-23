package com.sm.jeyz9.storemateapi.services.impl;

import com.sm.jeyz9.storemateapi.dto.ChangePasswordDTO;
import com.sm.jeyz9.storemateapi.dto.PasswordResetDTO;
import com.sm.jeyz9.storemateapi.models.PasswordResetToken;
import com.sm.jeyz9.storemateapi.repository.PasswordResetTokenRepository;
import com.sm.jeyz9.storemateapi.repository.RoleRepository;
import com.sm.jeyz9.storemateapi.repository.UserRepository;
import com.sm.jeyz9.storemateapi.config.JwtService;
import com.sm.jeyz9.storemateapi.dto.LoginDTO;
import com.sm.jeyz9.storemateapi.dto.RegisterDTO;
import com.sm.jeyz9.storemateapi.exceptions.WebException;
import com.sm.jeyz9.storemateapi.models.Role;
import com.sm.jeyz9.storemateapi.models.RoleName;
import com.sm.jeyz9.storemateapi.models.User;
import com.sm.jeyz9.storemateapi.services.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, RoleRepository roleRepository, JwtService jwtService, PasswordResetTokenRepository passwordResetTokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.jwtService = jwtService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    @Override
    public String register(RegisterDTO request) {
        try {
            if (userRepository.existsUserByEmail(request.getEmail())) {
                throw new WebException(HttpStatus.BAD_REQUEST, "Exist by email");
            }

            if (userRepository.existsUserByPhone(request.getPhone())) {
                throw new WebException(HttpStatus.BAD_REQUEST, "Exist by phone");
            }

            if (!request.getPassword().equals(request.getConfirmPassword())) {
                throw new WebException(HttpStatus.BAD_REQUEST, "Password does not match");
            }
            
           
            Role userRole = roleRepository.findByRoleName(RoleName.USER).orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "Role not found."));
            Set<Role> roles = Set.of(userRole);
            
            User user = User.builder()
                    .id(null)
                    .name(request.getName())
                    .email(request.getEmail())
                    .phone(request.getPhone())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .roles(roles)
                    .createdAt(LocalDateTime.now())
                    .build();
            userRepository.save(user);
            return "Register successfully";
        } catch (WebException e) {
            throw e;
        }catch (Exception e){
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error!");
        }
    }

    @Override
    public String login(LoginDTO request) {
        User user = userRepository.findUserByEmail(request.getEmail()).orElseThrow(() -> new WebException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        
        if(!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new WebException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                user.getRoles().stream()
                        .map(r -> new SimpleGrantedAuthority("ROLE_" + r.getRoleName())
                        ).toList()
        );
        
        return jwtService.generateToken(userDetails);
    }

    @Override
    @Transactional
    public String resetPassword(PasswordResetDTO request) {
        try{
            PasswordResetToken token = passwordResetTokenRepository.findByToken(request.getToken()).orElseThrow(() -> new WebException(HttpStatus.BAD_REQUEST, "This token is invalid."));
            
            if(token.getExpiredAt().isBefore(LocalDateTime.now())) {
                throw new WebException(HttpStatus.BAD_REQUEST, "Token is expired");
            }
            if (!request.getPassword().equals(request.getConfirmPassword())) {
                throw new WebException(HttpStatus.BAD_REQUEST, "Password does not match");
            }
            
            User user = userRepository.findById(token.getUser().getId()).orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "User not found"));
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            userRepository.save(user);
            
            token.setUsedAt(LocalDateTime.now());
            token.setExpiredAt(LocalDateTime.now());
            passwordResetTokenRepository.save(token);
            return "Reset password successfully.";
        } catch (WebException e) {
            throw e;
        }catch (Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "Server error: " + e.getMessage());
        }
    }

    @Override
    public String changePassword(String email, ChangePasswordDTO request) {
        try{
            User user = userRepository.findUserByEmail(email).orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "User not found."));
            if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
                throw new WebException(HttpStatus.BAD_REQUEST, "Old password is incorrect.");
            }
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                throw new WebException(HttpStatus.BAD_REQUEST, "Password does not match");
            }
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);
            return "Password changed successfully. Please login again.";
        }catch (WebException e) {
            throw e;
        }catch (Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "Server error: " + e.getMessage());
        }
    }
}
