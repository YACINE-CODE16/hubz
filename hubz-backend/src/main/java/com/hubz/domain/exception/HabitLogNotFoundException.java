package com.hubz.domain.exception;

import java.util.UUID;

public class HabitLogNotFoundException extends RuntimeException {
    public HabitLogNotFoundException(UUID id) {
        super("Habit log not found with id: " + id);
    }
}
