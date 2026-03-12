package com.microchaos.backend.model;

public enum CommunicationMode {
    SYNC,
    ASYNC;

    public static CommunicationMode from(String raw) {
        if (raw == null || raw.isBlank()) {
            return SYNC;
        }
        return CommunicationMode.valueOf(raw.trim().toUpperCase());
    }
}
