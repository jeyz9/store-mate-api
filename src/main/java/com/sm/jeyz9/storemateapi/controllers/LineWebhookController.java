package com.sm.jeyz9.storemateapi.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sm.jeyz9.storemateapi.config.LineConfig;
import com.sm.jeyz9.storemateapi.services.LineMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@RestController
@RequestMapping("/api/v1/line")
public class LineWebhookController {

    private final LineConfig lineConfig;
    private final LineMessageService lineMessageService;
    private final ObjectMapper objectMapper;

    @Autowired
    public LineWebhookController(LineConfig lineConfig,
                                 LineMessageService lineMessageService,
                                 ObjectMapper objectMapper) {
        this.lineConfig = lineConfig;
        this.lineMessageService = lineMessageService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(
            @RequestHeader("X-Line-Signature") String signature,
            @RequestBody String body) {

        /*if (!validateSignature(body, signature)) {
            return ResponseEntity.status(401).body("Invalid signature");
        }*/

        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode events = root.get("events");

            if (events != null && events.isArray()) {
                for (JsonNode event : events) {
                    lineMessageService.handleEvent(event);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return ResponseEntity.ok("OK");
    }

    private boolean validateSignature(String body, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(
                    lineConfig.getChannelSecret().getBytes(), "HmacSHA256");
            mac.init(key);
            byte[] hash = mac.doFinal(body.getBytes());
            String expected = Base64.getEncoder().encodeToString(hash);
            return expected.equals(signature);
        } catch (Exception e) {
            return false;
        }
    }
}