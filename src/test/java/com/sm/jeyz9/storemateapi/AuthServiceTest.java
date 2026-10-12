package com.sm.jeyz9.storemateapi;

import com.sm.jeyz9.storemateapi.config.JwtService;
import com.sm.jeyz9.storemateapi.dto.ChangePasswordDTO;
import com.sm.jeyz9.storemateapi.dto.LoginDTO;
import com.sm.jeyz9.storemateapi.dto.RegisterDTO;
import com.sm.jeyz9.storemateapi.exceptions.WebException;
import com.sm.jeyz9.storemateapi.models.User;
import com.sm.jeyz9.storemateapi.repository.PasswordResetTokenRepository;
import com.sm.jeyz9.storemateapi.repository.RoleRepository;
import com.sm.jeyz9.storemateapi.repository.UserRepository;
import com.sm.jeyz9.storemateapi.services.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthServiceTest {

    private RecordingUserRepository userRepository;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        userRepository = new RecordingUserRepository();
        PasswordEncoder passwordEncoder = new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return "encoded:" + rawPassword;
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return false;
            }
        };
        authService = new AuthServiceImpl(
                userRepository.asRepository(),
                passwordEncoder,
                proxy(RoleRepository.class, (proxy, method, args) -> Optional.empty()),
                null,
                proxy(PasswordResetTokenRepository.class, (proxy, method, args) -> Optional.empty())
        );
    }

    @Test
    void registerRejectsAnEmailThatAlreadyExists() {
        userRepository.emailExists = true;
        RegisterDTO request = new RegisterDTO();
        request.setEmail("existing@example.com");

        WebException exception = assertThrows(WebException.class, () ->
                authService.register(request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Exist by email", exception.getMessage());
        assertEquals(0, userRepository.phoneCheckCount);
        assertEquals(0, userRepository.saveCount);
    }

    @Test
    void loginRejectsUnknownEmailWithUnauthorized() {
        LoginDTO request = new LoginDTO();
        request.setEmail("unknown@example.com");
        request.setPassword("password");

        WebException exception = assertThrows(WebException.class, () ->
                authService.login(request)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertEquals("Invalid email or password", exception.getMessage());
        assertEquals("unknown@example.com", userRepository.requestedEmail);
    }

    @Test
    void changePasswordRejectsAnIncorrectCurrentPassword() {
        userRepository.user = Optional.of(User.builder()
                .id(7L)
                .email("user@example.com")
                .password("stored-password-hash")
                .build());
        ChangePasswordDTO request = new ChangePasswordDTO();
        request.setOldPassword("incorrect-password");
        request.setNewPassword("new-password");
        request.setConfirmPassword("new-password");

        WebException exception = assertThrows(WebException.class, () ->
                authService.changePassword("user@example.com", request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Old password is incorrect.", exception.getMessage());
        assertEquals(0, userRepository.saveCount);
    }

    private static class RecordingUserRepository implements InvocationHandler {
        private boolean emailExists;
        private int phoneCheckCount;
        private int saveCount;
        private String requestedEmail;
        private Optional<User> user = Optional.empty();

        private UserRepository asRepository() {
            return proxy(UserRepository.class, this);
        }

        @Override
        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
            return switch (method.getName()) {
                case "existsUserByEmail" -> emailExists;
                case "existsUserByPhone" -> {
                    phoneCheckCount++;
                    yield false;
                }
                case "findUserByEmail" -> {
                    requestedEmail = (String) args[0];
                    yield user;
                }
                case "save" -> {
                    saveCount++;
                    yield args[0];
                }
                default -> null;
            };
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }
}
