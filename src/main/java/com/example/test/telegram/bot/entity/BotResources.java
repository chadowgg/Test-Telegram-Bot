package com.example.test.telegram.bot.entity;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
@Data
@Component
public class BotResources {
    @Value("${bot.name}")
    private String botName;
    @Value("${bot.token}")
    private String token;
}
