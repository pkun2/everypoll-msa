package com.everypoll.authService.security;

import com.everypoll.authService.service.UserDetailsServiceImpl;
import com.everypoll.common.config.JwtUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final ApplicationContext context;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, ApplicationContext context) {
        this.jwtUtil = jwtUtil;
        this.context = context;
    }

    private UserDetailsServiceImpl getUserDetailsService() {
        return context.getBean(UserDetailsServiceImpl.class);
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);
            if (jwt != null && jwtUtil.validateToken(jwt)) {
                String userId = jwtUtil.getUserIdFromToken(jwt); 

                String tokenType = jwtUtil.getTokenType(jwt);
                if (!"access".equals(tokenType)) { // access 토큰만 인증에 사용되도록 설정
                    filterChain.doFilter(request, response);
                    return; 
                }

                UserDetails userDetails = getUserDetailsService().loadUserById(Long.parseLong(userId));
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            log.error("인증 실패!: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        return null;
    }
}