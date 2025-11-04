package com.everypoll.pollService.listener;

import com.everypoll.common.dto.CreatedUserEvent;
import com.everypoll.pollService.model.User;
import com.everypoll.pollService.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UserEventListener {

    private static final Logger logger = LoggerFactory.getLogger(UserEventListener.class);
    private final UserRepository userRepository;

    public UserEventListener(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    @KafkaListener(topics = "user-creation-topic", groupId = "poll-service-group")
    public void handleUserCreated(CreatedUserEvent event) {
        logger.info("이벤트 전달받음!: {}", event.getUserId());

        User userToReplicate = User.builder()
            .id(event.getUserId())
            .username(event.getUsername())    
        .build();

        userRepository.save(userToReplicate);
        logger.info("User {} 가 성공적으로 저장되었습니다.", event.getUserId());
    }
}
