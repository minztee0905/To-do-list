package com.example.ticktok.util;

import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.regex.Pattern;

public final class AuthAccountMapper {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]{3,30}$");
    private static final String USERNAME_EMAIL_DOMAIN = "ticktok.user";

    private AuthAccountMapper() {
    }

    @Nullable
    public static String toAuthEmail(@Nullable String accountInput) {
        if (accountInput == null) {
            return null;
        }

        String normalized = accountInput.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        if (normalized.contains("@")) {
            return normalized.toLowerCase(Locale.ROOT);
        }

        if (!USERNAME_PATTERN.matcher(normalized).matches()) {
            return null;
        }

        return normalized.toLowerCase(Locale.ROOT) + "@" + USERNAME_EMAIL_DOMAIN;
    }

    @Nullable
    public static String normalizeDisplayName(@Nullable String accountInput) {
        if (accountInput == null) {
            return null;
        }
        String normalized = accountInput.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}

