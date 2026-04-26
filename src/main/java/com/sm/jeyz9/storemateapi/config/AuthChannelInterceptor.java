package com.sm.jeyz9.storemateapi.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuthChannelInterceptor implements ChannelInterceptor {
    private final JwtService jwtService;
    
    @Autowired
    public AuthChannelInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }
    
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if(accessor == null) {
            return message;
        }

        // DEBUG
        log.info("Teest preSend: {}", accessor.getCommand());
        
        if(StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            
            if(authHeader == null || !authHeader.startsWith("Bearer ")) {
                // DEBUG
                log.info("Missing Authorization");
                return null;
            }
            
            String token = authHeader.substring(7);
            // DEBUG
            log.info("TEST TOKEN: {}", token);
            
            if(jwtService.isTokenExpired(token)) {
                throw new IllegalArgumentException("Token expired");
            }
            
            String email = jwtService.extractUsername(token);
            // DEBUG
            log.info("TEST EMAIL: {}", email);
            accessor.setUser(() -> email);
            
            return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
        }
        
        return message;
    }
}
