package com.everypoll.common.event.auth;

import java.util.List;

import com.everypoll.common.event.BaseEvent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class UserCreatedEvent extends BaseEvent {
    private Long userId;
    private String username;
    private String email;
    private List<String> roles;

    public static UserCreatedEvent of(Long userId, String username, String email, List<String> roles) {
        UserCreatedEvent event = UserCreatedEvent.builder()
            .userId(userId)
            .username(username)
            .email(email)
            .roles(roles)
            .build();
        event.init("USER_CREATED", "auth-service");
        return event;
    }
}
