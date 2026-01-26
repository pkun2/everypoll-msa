package com.everypoll.common.event.auth;

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
public class UserDeletedEvent extends BaseEvent {
    private Long userId;
    private String username;

    public static UserDeletedEvent of(Long userId, String username) {
        UserDeletedEvent event = UserDeletedEvent.builder()
            .userId(userId)
            .username(username)
            .build();
        event.init("USER_DELETED", "auth-service");
        return event;
    }
    
}
