package com.everypoll.common.security;

import com.everypoll.common.config.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtValidator {
    
    private final JwtUtil jwtUtil;

    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token);
    }

    public Authentication getAuthentication(String token) {
        return jwtUtil.getAuthentication(token);
    }
}
