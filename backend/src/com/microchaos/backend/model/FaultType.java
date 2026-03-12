package com.microchaos.backend.model;

public enum FaultType {
    LATENCY,
    ERROR_RESPONSE,
    TIMEOUT,
    DATABASE_CONNECTION,
    DEPENDENCY_UNAVAILABLE,
    CPU_SPIKE,
    THREAD_POOL_EXHAUSTION;

    public static FaultType from(String raw) {
        if (raw == null || raw.isBlank()) {
            return LATENCY;
        }
        return FaultType.valueOf(raw.trim().toUpperCase());
    }
}
