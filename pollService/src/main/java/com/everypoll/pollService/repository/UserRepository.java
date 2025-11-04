package com.everypoll.pollService.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.everypoll.pollService.model.User;

public interface UserRepository extends JpaRepository<User, String> {
}