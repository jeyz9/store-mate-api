package com.sm.jeyz9.storemateapi;

import com.sm.jeyz9.storemateapi.controllers.ProductController;
import com.sm.jeyz9.storemateapi.exceptions.ExceptionControllerAdvice;
import com.sm.jeyz9.storemateapi.services.ProductService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductControllerTest {

    private ValidatorFactory validatorFactory;
    private MockMvc mockMvc;
    private AtomicInteger serviceCalls;

    @BeforeEach
    void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        Validator validator = validatorFactory.getValidator();
        serviceCalls = new AtomicInteger();
        ProductService productService = proxy(ProductService.class, (proxy, method, args) -> {
            if (method.getName().equals("addProduct") || method.getName().equals("updateProduct")) {
                serviceCalls.incrementAndGet();
                return method.getName().equals("addProduct") ? "Add product success." : "Update product success.";
            }
            return null;
        });
        mockMvc = MockMvcBuilders.standaloneSetup(new ProductController(productService, validator))
                .setControllerAdvice(new ExceptionControllerAdvice())
                .build();
    }

    @AfterEach
    void tearDown() {
        validatorFactory.close();
    }

    @Test
    void addProductRejectsBlankNameBeforeCallingService() throws Exception {
        String requestJson = """
                {
                  "productName": "   ",
                  "categoryId": 3,
                  "price": 159.00,
                  "statusId": 1,
                  "description": "Mango drink",
                  "stockQuantity": 100
                }
                """;

        mockMvc.perform(multipart("/api/v1/moderator/products")
                        .file(new MockMultipartFile(
                                "request", "", MediaType.TEXT_PLAIN_VALUE,
                                requestJson.getBytes(StandardCharsets.UTF_8)))
                        .file(new MockMultipartFile(
                                "files", "product.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[]{1, 2, 3})))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Product name is required"));

        assertEquals(0, serviceCalls.get());
    }

    @Test
    void updateProductRejectsEmptyOrWhitespaceNameBeforeCallingService() throws Exception {
        for (String productName : new String[]{"", "   "}) {
            String requestJson = """
                    {
                      "productName": "%s",
                      "categoryId": 3,
                      "price": 159.00,
                      "statusId": 1,
                      "description": "Mango drink",
                      "stockQuantity": 100,
                      "removeImages": []
                    }
                    """.formatted(productName);

            mockMvc.perform(multipart("/api/v1/moderator/products/96")
                            .file(new MockMultipartFile(
                                    "request", "", MediaType.TEXT_PLAIN_VALUE,
                                    requestJson.getBytes(StandardCharsets.UTF_8)))
                            .file(new MockMultipartFile(
                                    "files", "product.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[]{1, 2, 3}))
                            .with(request -> {
                                request.setMethod("PUT");
                                return request;
                            }))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Product name is required"));
        }

        assertEquals(0, serviceCalls.get());
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }
}
