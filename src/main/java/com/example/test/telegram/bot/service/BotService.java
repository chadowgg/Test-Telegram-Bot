package com.example.test.telegram.bot.service;

import com.example.test.telegram.bot.OpenAiAnalyze;
import org.springframework.stereotype.Service;

@Service
public class BotService {
    private final OpenAiAnalyze openAiService;

    public BotService(OpenAiAnalyze openAiService) {
        this.openAiService = openAiService;
    }

    public String analyzeFeedback(String text) {
        String result = null;
        try {
            result = openAiService.analyzeSentiment(text);
        } catch (Exception e) {
            System.out.println("Помилка аналізу OpenAI: " + e.getMessage());
        }
        return result;
    }


}
