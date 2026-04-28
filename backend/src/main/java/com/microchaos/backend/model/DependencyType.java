package com.microchaos.backend.model;

public enum DependencyType {
    REST,
    DB,
    CACHE,
    QUEUE,
    AUTH;

    public static DependencyType from(String raw) {
        if (raw == null || raw.isBlank()) {
            return REST;
        }
        return DependencyType.valueOf(raw.trim().toUpperCase());
    }
}
