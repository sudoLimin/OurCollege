package com.example.demo.util;

import java.util.regex.Pattern;

public class InputSanitizer {

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");

    private static final Pattern MULTI_SPACE_PATTERN = Pattern.compile("\\s+");

    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }

        String sanitized = HTML_TAG_PATTERN.matcher(input).replaceAll("");

        sanitized = MULTI_SPACE_PATTERN.matcher(sanitized).replaceAll(" ");

        return sanitized.trim();
    }

    public static String sanitizeNonEmpty(String input) {
        String sanitized = sanitize(input);
        if (sanitized == null || sanitized.isEmpty()) {
            return null;
        }
        return sanitized;
    }

    public static String sanitizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase();
    }
}
