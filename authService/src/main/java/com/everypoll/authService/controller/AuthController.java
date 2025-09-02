package com.everypoll.authService.controller;

import com.everypoll.authService.dto.LoginRequest;
import com.everypoll.authService.dto.LoginResponse;
import com.everypoll.authService.dto.MessageResponse;
import com.everypoll.authService.dto.SignUpRequest;
import com.everypoll.authService.dto.RefreshTokenRequest;
import com.everypoll.authService.security.UserDetailsImpl;
import com.everypoll.authService.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final static Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        logger.info(request.getUsername(), ": try to login");
        LoginResponse loginResponse = authService.login(request);

        ResponseCookie cookie = ResponseCookie.from("refreshToken", loginResponse.getRefreshToken())
            .maxAge(refreshExpirationMs / 1000)
            .path("/")
            .secure(true)
            .sameSite("None")
            .httpOnly(true)
            .build();

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(loginResponse);
    }

    @PostMapping("/signup")
    public ResponseEntity<LoginResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        logger.info(request.getUsername());
        LoginResponse signuResponse = authService.signUpAndLogin(request);

        return ResponseEntity.ok(signuResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        if(userDetails == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("사용자를 찾을 수 없습니다."));
        }

        String userId = userDetails.getId().toString();
        authService.logout(userId);
        logger.info("logout: ", userId);

        return ResponseEntity.ok(new MessageResponse("성공적으로 로그아웃 되었습니다."));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refreshAccessToken(@RequestBody RefreshTokenRequest request) {
        LoginResponse response = authService.refreshAccessToken(request.getRefreshToken());

        ResponseCookie cookie = ResponseCookie.from("refreshToken", response.getRefreshToken())
                .maxAge(refreshExpirationMs / 1000)
                .path("/")
                .secure(true)
                .sameSite("None")
                .httpOnly(true)
                .build();
        
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);

    }
}
