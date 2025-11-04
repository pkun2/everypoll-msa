package com.everypoll.pollService.config;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component 
public class AuditorAwareImpl implements AuditorAware<String> { 

    @Override
    public Optional<String> getCurrentAuditor() {
        // 현재 인증 정보 확인
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 인증 정보가 없거나, 인증되지 않았거나, 익명 사용자인 경우
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.empty();
        }

        return Optional.of(authentication.getName());
    }
}
