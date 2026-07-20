package com.everypoll.pollService.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;
import java.util.Set;

import com.everypoll.pollService.config.FeignConfig;

@FeignClient(name = "authService", url = "${auth.service.url:http://auth-service:8081}", configuration = FeignConfig.class) // authorization 헤더값 전달
public interface AuthServiceClient {

    @PostMapping("/api/auth/users/names")
    Map<Long, String> getUsersNames(@RequestBody Set<Long> userIds);
}
