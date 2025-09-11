package com.example.test.telegram.bot.repository;

import com.example.test.telegram.bot.entity.User;
import com.example.test.telegram.bot.enums.Affiliate;
import com.example.test.telegram.bot.enums.Position;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findById(Long id);
    boolean existsById(Long id);

    List<User> findAllByAffiliate(Affiliate affiliate);
    List<User> findAllByPosition(Position position);
}
