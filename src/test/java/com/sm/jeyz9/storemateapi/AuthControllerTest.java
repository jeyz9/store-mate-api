package com.sm.jeyz9.storemateapi;

import com.sm.jeyz9.storemateapi.controllers.AuthController;
import com.sm.jeyz9.storemateapi.dto.ChangePasswordDTO;
import com.sm.jeyz9.storemateapi.dto.LoginDTO;
import com.sm.jeyz9.storemateapi.dto.PasswordResetDTO;
import com.sm.jeyz9.storemateapi.dto.RegisterDTO;
import com.sm.jeyz9.storemateapi.exceptions.ExceptionControllerAdvice;
import com.sm.jeyz9.storemateapi.services.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private MockMvc mockMvc;
    private RecordingAuthService authService;

    @BeforeEach
    void setUp() {
        authService = new RecordingAuthService();
        AuthController controller = new AuthController(authService, null);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ExceptionControllerAdvice())
                .build();
    }

    @Test
    void registerRejectsPhoneNumberWithFewerThanTenDigits() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Test User",
                                  "email": "test@example.com",
                                  "phone": "08123444",
                                  "password": "password1",
                                  "confirmPassword": "password1"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.phone", is("Invalid phone number")));

        assertEquals(0, authService.registerCallCount);
    }

    @Test
    void registerAcceptsTenDigitPhoneNumber() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Test User",
                                  "email": "test@example.com",
                                  "phone": "0812345678",
                                  "password": "Password1!",
                                  "confirmPassword": "Password1!"
                                }
                                """))
                .andExpect(status().isCreated());

        assertEquals(1, authService.registerCallCount);
        assertEquals("0812345678", authService.registeredPhone);
    }

    @Test
    void registerRejectsPasswordWithoutUppercaseLetter() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Test User",
                                  "email": "test@example.com",
                                  "phone": "0812345678",
                                  "password": "abcd12345",
                                  "confirmPassword": "abcd12345"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password", is("Password does not meet requirements")));

        assertEquals(0, authService.registerCallCount);
    }

    @Test
    void registerRejectsPasswordContainingThaiDigits() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Test User",
                                  "email": "test@example.com",
                                  "phone": "0812345678",
                                  "password": "Password๑",
                                  "confirmPassword": "Password๑"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password", is("Password does not meet requirements")));

        assertEquals(0, authService.registerCallCount);
    }

    private static class RecordingAuthService implements AuthService {
        private int registerCallCount;
        private String registeredPhone;

        @Override
        public String register(RegisterDTO request) {
            registerCallCount++;
            registeredPhone = request.getPhone();
            return "Register successfully";
        }

        @Override
        public String login(LoginDTO request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String resetPassword(PasswordResetDTO request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String changePassword(String email, ChangePasswordDTO request) {
            throw new UnsupportedOperationException();
        }
    }
}
