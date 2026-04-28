package com.microchaos.backend.model;

public enum RemediationMode {
    MANUAL,
    SEMI_AUTOMATIC,
    AUTOMATIC;

    public static RemediationMode from(String raw) {
        if (raw == null || raw.isBlank()) {
            return MANUAL;
        }
        return RemediationMode.valueOf(raw.trim().toUpperCase());
    }
}
