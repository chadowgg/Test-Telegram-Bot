package com.example.test.telegram.bot.repository;

import com.example.test.telegram.bot.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Integer> {
    List<Feedback> findAllByUserId(Long user_id);
    List<Feedback> findAllByCriticality(Integer level);
}
