package com.example.test.telegram.bot.entity;

import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TrelloCard {

    private final String key;
    private final String token;
    private final String list;

    public TrelloCard(@Value("${trello.api.key}") String key,
                      @Value("${trello.token}") String token,
                      @Value("${trello.id.list}") String list) {
        this.key = key;
        this.token = token;
        this.list = list;
    }

    public void createCard(String feedback, String recommendation) {
         JSONObject response = Unirest.post("https://api.trello.com/1/cards")
                .queryString("key", key)
                .queryString("token", token)
                .queryString("idList", list)
                .queryString("name", feedback)
                .queryString("desc", recommendation)
                .asJson()
                .getBody()
                .getObject();

        System.out.println("Картку створено ID: " + response.getString("id"));
        System.out.println("Посилання: " + response.getString("shortUrl"));
    }
}
