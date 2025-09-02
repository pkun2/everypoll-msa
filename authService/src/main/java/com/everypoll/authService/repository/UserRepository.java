package com.everypoll.authService.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.everypoll.authService.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
