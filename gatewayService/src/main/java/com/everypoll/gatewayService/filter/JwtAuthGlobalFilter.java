package com.everypoll.gatewayService.filter;

import com.everypoll.common.config.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    private static final List<String> PUBLIC_POST_PATHS = List.of(
        "/api/auth/login", "/api/auth/signup", "/api/auth/refresh"
    );

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path   = request.getURI().getPath();
        String method = request.getMethod().name();

        // X-User-Id 스푸핑 차단 — 인증 결과와 무관하게 항상 제거
        ServerHttpRequest.Builder mutated = request.mutate()
            .headers(h -> h.remove("X-User-Id"));

        if (isPublicRoute(method, path)) {
            return chain.filter(exchange.mutate().request(mutated.build()).build());
        }

        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        if (!jwtUtil.validateToken(token)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        if (!"access".equals(jwtUtil.getTokenType(token))) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String userId = jwtUtil.getUserIdFromToken(token);
        ServerHttpRequest forwarded = mutated.header("X-User-Id", userId).build();

        return chain.filter(exchange.mutate().request(forwarded).build());
    }

    private boolean isPublicRoute(String method, String path) {
        if ("POST".equals(method) && PUBLIC_POST_PATHS.contains(path)) return true;
        if ("GET".equals(method) && path.startsWith("/api/polls")) return true;
        if ("GET".equals(method) && path.matches("/api/votes/polls/\\d+/(results|stats)")) return true;
        if ("PATCH".equals(method) && path.matches("/api/polls/\\d+/blind")) return true;
        return false;
    }
}
