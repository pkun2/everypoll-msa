package com.everypoll.authService.service;

import java.util.List;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.everypoll.authService.dto.LoginRequest;
import com.everypoll.authService.dto.LoginResponse;
import com.everypoll.authService.dto.SignUpRequest;
import com.everypoll.authService.model.User;
import com.everypoll.authService.repository.UserRepository;
import com.everypoll.authService.security.UserDetailsImpl;
import com.everypoll.common.config.JwtUtil;
import com.everypoll.common.dto.CreatedUserEvent;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final KafkaProducerService kafkaProducerService;

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        String userId = userDetails.getId().toString();
        List<String> roles = userDetails.getAuthorities().stream()
                                        .map(grantedAuthority -> grantedAuthority.getAuthority())
                                        .toList();
        String accessToken = jwtUtil.generateAccessToken(userId, roles);
        String refreshToken = jwtUtil.generateRefreshToken(userId);

        refreshTokenService.saveToken(userId, refreshToken);

        CreatedUserEvent event = CreatedUserEvent.builder()
            .userId(userId)
            .username(userDetails.getUsername())    
            .build();

        kafkaProducerService.sendUserCreationEvent(event);

        return LoginResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .username(userDetails.getUsername())
            .userId(userId)
            .roles(roles)
            .build();
    }

    public LoginResponse refreshAccessToken(String refreshToken) {
        if(!jwtUtil.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }

        // 토큰 타입이 refresh인지 확인 
        String tokenType = jwtUtil.getTokenType(refreshToken);        
        if(!tokenType.equals("refresh")) {
            throw new IllegalArgumentException("refresh token이 아닙니다.");
        }

        String userId = jwtUtil.getUserIdFromToken(refreshToken);
        String storedRefreshToken = refreshTokenService.getToken(userId);

        // redis의 refresh token과 요청 받은 refresh token이 같은 토큰인지 확인
        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
            // 토큰 탈취 가능성 고려, 토큰 삭제            
            refreshTokenService.deleteToken(userId);

            throw new IllegalArgumentException("저장된 토큰과 일치하지 않습니다. 재로그인이 필요합니다.");
        }

        User user = userRepository.findById(Long.parseLong(userId))
            .orElseThrow(() -> new UsernameNotFoundException("해당하는 사용자를 찾을 수 없습니다."));

        // 토큰 다시 만들기
        String reAccessToken = jwtUtil.generateAccessToken(user.getId().toString(), user.getRoles());
        String reRefreshToken = jwtUtil.generateRefreshToken(user.getId().toString());
        refreshTokenService.saveToken(user.getId().toString(), reRefreshToken);

        return LoginResponse.builder()
            .accessToken(reAccessToken)
            .refreshToken(reRefreshToken)
            .userId(user.getId().toString())
            .username(user.getUsername())
            .roles(user.getRoles())
            .build();
    }

    public LoginResponse signUpAndLogin(SignUpRequest request) {
        // 아이디 중복 확인
        if(userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("이미 사용중인 아이디 입니다.");
        }

        // 이메일 중복 확인
        if(userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용중인 이메일 입니다.");
        }

        // 비밀번호 일치 확인
        if(!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("비밀번호가 서로 일치하지 않습니다.");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User newUser = User.builder()
            .username(request.getUsername())
            .password(encodedPassword)
            .email(request.getEmail())
            .roles(List.of("ROLE_USER"))
            .build();
        
        User savedUser = userRepository.save(newUser);

        String accessToken = jwtUtil.generateAccessToken(savedUser.getId().toString(), savedUser.getRoles());
        String refreshToken = jwtUtil.generateRefreshToken(savedUser.getId().toString());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(savedUser.getId().toString())
                .username(savedUser.getUsername())
                .roles(savedUser.getRoles())
                .build();


    }

    public void logout(String userId) {
        refreshTokenService.deleteToken(userId);
    }
}