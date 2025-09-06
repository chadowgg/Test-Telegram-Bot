package com.example.test.telegram.bot.service;

import com.example.test.telegram.bot.entity.TrelloCard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TrelloService {
    private final TrelloCard trelloCard;

    public void createTask(String feedback, String recommendation) {
        try {
            trelloCard.createCard(feedback, recommendation);
        } catch (Exception e) {
            System.out.println("Помилка створення задачі: " + e.getMessage());
        }
    }
}
