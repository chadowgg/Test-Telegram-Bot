package com.example.test.telegram.bot.enums;

public enum Position {
    MECHANIC("Механік"),
    ELECTRICIAN("Електрик"),
    MANAGE("Менеджер");

    private final String displayName;

    Position(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
