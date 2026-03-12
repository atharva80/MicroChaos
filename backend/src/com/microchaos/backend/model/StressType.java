package com.microchaos.backend.model;

public enum StressType {
    BURST,
    RAMP_UP;

    public static StressType from(String raw) {
        if (raw == null || raw.isBlank()) {
            return BURST;
        }
        return StressType.valueOf(raw.trim().toUpperCase());
    }
}
