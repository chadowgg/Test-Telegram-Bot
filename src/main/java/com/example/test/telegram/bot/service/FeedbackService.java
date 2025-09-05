package com.example.test.telegram.bot.service;

import com.example.test.telegram.bot.entity.Feedback;
import com.example.test.telegram.bot.repository.FeedbackRepository;
import org.springframework.stereotype.Service;

@Service
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;

    public FeedbackService(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    public void saveFeedbackFromAI(String responseBody) {
        String[] lines = responseBody.split("\n");

        String feedback = "";
        int criticality = 0;
        String recommendation = "";

        for (String line : lines) {
            System.out.println(line);
            if (line.startsWith("Відгук:")) {
                feedback = line.split(":")[1].trim();
            } else if (line.startsWith("Критичність:")) {
                criticality = Integer.parseInt(line.split(":")[1].trim());
            } else if (line.startsWith("Рекомендація:")) {
                recommendation = line.split(":")[1].trim();
            }
        }

        Feedback userFeedback = new Feedback();
        userFeedback.setFeedback(feedback);
        userFeedback.setCriticality(criticality);
        userFeedback.setRecommendation(recommendation);

        feedbackRepository.save(userFeedback);
    }
}
