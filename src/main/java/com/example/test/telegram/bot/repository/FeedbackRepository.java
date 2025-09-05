package com.example.test.telegram.bot.repository;

import com.example.test.telegram.bot.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Integer> {
}
