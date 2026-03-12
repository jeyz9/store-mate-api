package com.sm.jeyz9.storemateapi.config;

import com.sm.jeyz9.storemateapi.exceptions.WebException;
import com.sm.jeyz9.storemateapi.models.Role;
import com.sm.jeyz9.storemateapi.models.User;
import com.sm.jeyz9.storemateapi.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.stream.Collectors;

@Service
public class JwtService {
    private final UserRepository userRepository;
    @Value("${jwt-secret}")
    private String secretKey;
    
    @Value("${jwt-expiration}")
    private long expiration;

    public JwtService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    private Key getSignKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }
    
    public String generateToken(UserDetails userDetails) {
        User user = userRepository.findUserByEmail(userDetails.getUsername()).orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "User not found"));
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .claim("userId", user.getId())
                .claim("roles", user.getRoles().stream().map(Role::getRoleName).collect(Collectors.toSet()))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignKey())
                .compact();
    }
    
    public String extractUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
    
    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }
    
    public boolean isTokenExpired(String token) {
        Date exp = Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
        return exp.before(new Date());
    }
}
