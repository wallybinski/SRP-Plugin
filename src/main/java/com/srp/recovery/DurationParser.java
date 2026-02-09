package com.srp.recovery;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

public final class DurationParser {
    private DurationParser() {
    }

    public static Optional<Duration> parseDuration(String input) {
        if (input == null || input.isEmpty()) {
            return Optional.empty();
        }
        String trimmed = input.trim().toLowerCase(Locale.ROOT);
        if (trimmed.length() < 2) {
            return Optional.empty();
        }
        char suffix = trimmed.charAt(trimmed.length() - 1);
        String numberPortion = trimmed.substring(0, trimmed.length() - 1);
        long value;
        try {
            value = Long.parseLong(numberPortion);
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
        if (value <= 0) {
            return Optional.empty();
        }
        return switch (suffix) {
            case 's' -> Optional.of(Duration.ofSeconds(value));
            case 'm' -> Optional.of(Duration.ofMinutes(value));
            case 'h' -> Optional.of(Duration.ofHours(value));
            default -> Optional.empty();
        };
    }
}
