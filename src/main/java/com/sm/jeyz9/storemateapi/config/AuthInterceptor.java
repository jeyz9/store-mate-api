package com.sm.jeyz9.storemateapi.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;

@Component
public class AuthInterceptor implements HandshakeInterceptor {
    
    private final JwtService jwtService;
    
    @Autowired
    public AuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }
    
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        List<String> authHeaders = request.getHeaders().get("Authorization");
        if(authHeaders == null || authHeaders.isEmpty()) {
            return false;
        }

        String authHeader = authHeaders.get(0);
        if (!authHeader.startsWith("Bearer ")) {
            return false;
        }

        String token = authHeader.substring(7);

        if(jwtService.isTokenExpired(token)){
            return false;
        }

        String email = jwtService.extractUsername(token);
        attributes.put("email", email);
        return true;
    }

    // TODO: TEST BYPASS
//    @Override
//    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) {
//        attributes.put("email", "test2@gmail.com");
//        return true;
//    }
    
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
    }
}
