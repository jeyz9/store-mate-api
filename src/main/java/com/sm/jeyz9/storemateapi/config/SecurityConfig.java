package com.sm.jeyz9.storemateapi.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtFilter;
    
    @Autowired
    public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            res.setStatus(401);
                            res.setContentType("application/json");
                            res.getWriter().write("""
                    {
                      "status": 401,
                      "message": "Unauthorized or invalid token"
                    }
                    """);
                        })
                        .accessDeniedHandler((req, res, e) -> {
                            res.setStatus(403);
                            res.setContentType("application/json");
                            res.getWriter().write("""
                    {
                      "status": 403,
                      "message": "Access denied"
                    }
                    """);
                        })
                )
                .authorizeHttpRequests((authorize) -> 
                        authorize
                                .requestMatchers(HttpMethod.GET,
                                        "/v3/api-docs/**",
                                        "/swagger-ui.html",
                                        "/swagger-ui/**",
                                        "/api/v1/products/grouped-by-category",
                                        "/api/v1/products/search",
                                        "/api/v1/products/{id}"
                                ).permitAll()
                                .requestMatchers(HttpMethod.POST,
                                        "/api/v1/auth/login",
                                        "/api/v1/auth/register",
                                        "/api/v1/auth/forgot-password",
                                        "/api/v1/auth/reset-password",
                                        
                                        // TODO: for test
                                        "/api/v1/moderator/products"
                                ).permitAll()
                                
                                .requestMatchers(HttpMethod.GET, 
                                        "/api/v1/cart/items",
                                        "/api/v1/users/me/overview")
                                .authenticated()
                                
                                .requestMatchers(HttpMethod.POST,
                                        "/api/v1/auth/change-password",
                                        "/api/v1/cart/items"
                                ).authenticated()

                                .requestMatchers(HttpMethod.PUT,
                                        "/api/v1/users/me/overview")
                                .authenticated()
                                
                                .requestMatchers(HttpMethod.PATCH, 
                                        "/api/v1/cart/items/{productId}/decrement",
                                        "/api/v1/cart/items/{productId}/increment"
                                ).authenticated()
                                
                                .requestMatchers(HttpMethod.DELETE, "/api/v1/cart/items/{productId}").authenticated()
                                
                                .anyRequest().authenticated()
                )
                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );
        return http.build();
    }
}
