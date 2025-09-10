package com.example.test.telegram.bot.enums;

public enum Affiliate {
    KYIV("Київ"),
    LVIV("Львів"),
    ZAPORIZHIA("Запоріжжя"),
    DNIPRO("Дніпро"),
    UZHHOROD("Ужгород");

    private final String displayName;

    Affiliate(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
