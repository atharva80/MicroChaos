package com.microchaos.backend.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class RemediationPolicy implements JsonEntity {
    private final long id;
    private final long projectId;
    private final String name;
    private final RemediationMode mode;
    private boolean active;
    private final Instant createdAt;

    public RemediationPolicy(long id, long projectId, String name, RemediationMode mode, boolean active) {
        this.id = id;
        this.projectId = projectId;
        this.name = name;
        this.mode = mode;
        this.active = active;
        this.createdAt = Instant.now();
    }

    public long getId() {
        return id;
    }

    public long getProjectId() {
        return projectId;
    }

    public String getName() {
        return name;
    }

    public RemediationMode getMode() {
        return mode;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("projectId", projectId);
        map.put("name", name);
        map.put("mode", mode.name());
        map.put("isActive", active);
        map.put("createdAt", createdAt.toString());
        return map;
    }
}
