package com.hubz.domain.enums;

/**
 * Supported languages for user preferences.
 */
public enum Language {
    FR("fr", "Francais"),
    EN("en", "English");

    private final String code;
    private final String displayName;

    Language(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }
}
