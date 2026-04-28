package com.microchaos.backend.model;

public enum RemediationActionType {
    RESTART_SERVICE,
    ENABLE_FALLBACK,
    REDUCE_TRAFFIC,
    STOP_EXPERIMENT;

    public static RemediationActionType from(String raw) {
        if (raw == null || raw.isBlank()) {
            return ENABLE_FALLBACK;
        }
        return RemediationActionType.valueOf(raw.trim().toUpperCase());
    }
}
