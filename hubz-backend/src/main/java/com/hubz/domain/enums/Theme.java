package com.hubz.domain.enums;

/**
 * Theme preferences for the UI.
 */
public enum Theme {
    SYSTEM("system", "System"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark");

    private final String code;
    private final String displayName;

    Theme(String code, String displayName) {
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
