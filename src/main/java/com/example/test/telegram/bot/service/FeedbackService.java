package com.example.test.telegram.bot.service;

import com.example.test.telegram.bot.entity.Feedback;
import com.example.test.telegram.bot.entity.User;
import com.example.test.telegram.bot.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final TrelloService trelloService;

    public void saveFeedbackFromAI( String feedback, String responseBody, User user) {
        String[] lines = responseBody.split("\n");

        String status = "";
        int criticality = 0;
        String recommendation = "";

        for (String line : lines) {
            if (line.startsWith("Відгук:")) {
                status = line.split(":")[1].trim();
            } else if (line.startsWith("Критичність:")) {
                criticality = Integer.parseInt(line.split(":")[1].trim());
            } else if (line.startsWith("Рекомендація:")) {
                recommendation = line.split(":")[1].trim();
            }
        }

        // Створення trello-карти якщо критичність 4 або 5
        if (criticality > 3) {
            trelloService.createTask(feedback, recommendation);
        }

        Feedback userFeedback = new Feedback();
        userFeedback.setFeedback(status);
        userFeedback.setUser(user);
        userFeedback.setCriticality(criticality);
        userFeedback.setRecommendation(recommendation);

        feedbackRepository.save(userFeedback);
    }
}
