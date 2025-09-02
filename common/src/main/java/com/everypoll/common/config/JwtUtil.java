package com.everypoll.common.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.security.Key;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-expiration-ms}")
    private long accessExpirationMs;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    private Key key;


    private Logger logger = LoggerFactory.getLogger(this.getClass());

    // 키 값을 서버 실행 시 한번만 불러옴
    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    // 액세스 토큰 발급 
    public String generateAccessToken(String userId, List<String> roles) {
        Claims claims = Jwts.claims().setSubject(userId); // 사용자의 id
        claims.put("roles", roles); // 사용자 정의 클레임: 사용자의 권한 정보, admin, user...
        claims.put("type", "access"); // 토큰 타입 명시, refresh token으로 로그아웃이 됐음

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessExpirationMs);

        // jjwt 빌더를 사용하여 JWT 생성
        return Jwts.builder()
                .setClaims(claims)
                .setIssuer("everypoll-auth-service") // iss: 토큰을 발급한 서비스
                .setIssuedAt(now) // iat: 토큰 발급 시간
                .setExpiration(expiryDate) // exp: 토큰 만료 시간
                .signWith(key, SignatureAlgorithm.HS512) // 서명: 사용할 키와 암호화 알고리즘 설정
                .compact(); // JWT 문자열로 압축
    }

    // 토큰 재발급
    public String generateRefreshToken(String userId) {
        Claims claims = Jwts.claims().setSubject(userId);
        claims.put("type", "refresh");

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpirationMs);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key)
                .compact();
    }

    // token의 타입 조회
    public String getTokenType(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                   .parseClaimsJws(token).getBody().get("type", String.class);
    }

    

    // 토큰에서 사용자 id 가져오기
    public String getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    // 토큰 유효성 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            logger.info("Invalid JWT Token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.info("Expired JWT Token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.info("Unsupported JWT Token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.info("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get("roles").toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        // userdetail 생성
        UserDetails principal = new User(claims.getSubject(), "", authorities);

        // Authentication 생성후 반환
        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }
}
