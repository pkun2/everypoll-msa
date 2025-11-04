package com.everypoll.authService.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RedisTemplate<String, String> redisTemplate;

    // Refresh Token 저장
    public void saveToken(String userId, String refreshToken) {
        // Refresh Token 구분을 위한 약자
        String key = "RT:" + userId;
        redisTemplate.opsForValue().set(key, refreshToken, Duration.ofDays(7));
    }

    // Refresh Token 조회
    public String getToken(String userId) {
        String key = "RT:" + userId;
        return redisTemplate.opsForValue().get(key);
    }

    // Refresh Token 삭제 
    public void deleteToken(String userId) {
        String key = "RT:" + userId;
        redisTemplate.delete(key);
    }
}
