package com.microchaos.backend.model;

public enum Criticality {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public static Criticality from(String raw) {
        if (raw == null || raw.isBlank()) {
            return MEDIUM;
        }
        return Criticality.valueOf(raw.trim().toUpperCase());
    }
}
