package com.sm.jeyz9.storemateapi.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

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
        
        if(accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            
            if(authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new IllegalArgumentException("Missing Authorization");
            }
            
            String token = authHeader.substring(7);
            
            if(jwtService.isTokenExpired(token)) {
                throw new IllegalArgumentException("Token expired");
            }
            
            String email = jwtService.extractUsername(token);
            accessor.setUser(() -> email);
        }
        
        return message;
    }
}
