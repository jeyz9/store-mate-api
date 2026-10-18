package com.sm.jeyz9.storemateapi.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;
import io.jsonwebtoken.JwtException;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    
    @Autowired
    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getServletPath().startsWith("/ws")||
                request.getServletPath().equals("/api/v1/line/webhook");
    }
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getServletPath();
        if (path.startsWith("/api/v1/stripe/webhook") ||
                path.equals("/api/v1/line/webhook")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (path.startsWith("/ws")) {
            filterChain.doFilter(request, response);
            return;
        }


        
        String authHeader = request.getHeader("Authorization");
        if(authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String jwt = authHeader.substring(7);
        String username;

        try {
            username = jwtService.extractUsername(jwt);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Rejected JWT for {} {} while reading subject ({})",
                    request.getMethod(), path, e.getClass().getSimpleName());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("""
                    {
                      "status": 401,
                      "message": "Unauthorized or invalid token"
                    }
                    """);
            return;
        }
        
        if(username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            try {
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } else {
                    log.warn("Rejected JWT for {} {} because validation returned false",
                            request.getMethod(), path);
                }
            } catch (JwtException | IllegalArgumentException e) {
                log.warn("Rejected JWT for {} {} during validation ({})",
                        request.getMethod(), path, e.getClass().getSimpleName());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("""
                        {
                          "status": 401,
                          "message": "Unauthorized or invalid token"
                        }
                        """);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
