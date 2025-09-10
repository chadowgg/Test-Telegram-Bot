package com.example.test.telegram.bot.repository;

import com.example.test.telegram.bot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findById(Long id);
    boolean existsById(Long id);
}
