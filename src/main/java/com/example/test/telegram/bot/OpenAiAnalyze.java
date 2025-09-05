package com.example.test.telegram.bot;

import com.example.test.telegram.bot.service.FeedbackService;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OpenAiAnalyze {

    private final String apiKey;
    private final OkHttpClient client = new OkHttpClient();
    private final FeedbackService feedbackService;

    public OpenAiAnalyze(@Value("${openai.api.key}") String apiKey,
                         FeedbackService feedbackService) {
        this.apiKey = apiKey;
        this.feedbackService = feedbackService;
    }

    public String analyzeSentiment(String text) throws IOException {
        // Системне повідомлення GPT
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content",
                "Ти асистент для аналізу настрою повідомлень. " +
                        "Класифікуй повідомлення користувача як Позитивне, Нейтральне або Негативне, " +
                        "та присвой рівень критичності від 1 (низька) до 5 (висока). " +
                        "Поверни результат у форматі: Відгук: <Позитивний/Нейтральний/Негативний>\n" +
                        "Критичність: <1-5>\n" +
                        "Рекомендація: <як можна вирішити дане питання>"
        );

        // Повідомлення користувача
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", text);

        // Формування повідомлень
        JSONArray messages = new JSONArray();
        messages.put(systemMessage);
        messages.put(userMessage);

        // JSON тіла запиту
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("model", "gpt-3.5-turbo");
        jsonBody.put("messages", messages);
        jsonBody.put("temperature", 0);

        // HTTP-запит
        RequestBody body = RequestBody.create(
                jsonBody.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .post(body)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        // Виконання запиту та обробка відповіді
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Неочікуваний код: " + response);
            }

            String responseBody = response.body().string().trim();
            JSONObject obj = new JSONObject(responseBody);

            // Отримуємо тільки content з відповіді
            String aiContent = obj.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim();

            feedbackService.saveFeedbackFromAI(aiContent);

            return aiContent;
        }
    }
}

