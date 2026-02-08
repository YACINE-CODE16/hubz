package com.hubz.domain.enums;

/**
 * Date format preferences.
 */
public enum DateFormat {
    DD_MM_YYYY("DD/MM/YYYY", "31/12/2024"),
    MM_DD_YYYY("MM/DD/YYYY", "12/31/2024"),
    YYYY_MM_DD("YYYY-MM-DD", "2024-12-31");

    private final String pattern;
    private final String example;

    DateFormat(String pattern, String example) {
        this.pattern = pattern;
        this.example = example;
    }

    public String getPattern() {
        return pattern;
    }

    public String getExample() {
        return example;
    }
}
