package com.microchaos.backend.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class TargetService implements JsonEntity {
    private final long id;
    private final long projectId;
    private String name;
    private String baseUrl;
    private String healthEndpoint;
    private String environment;
    private int timeoutThresholdMs;
    private ServiceStatus status;

    public TargetService(
        long id,
        long projectId,
        String name,
        String baseUrl,
        String healthEndpoint,
        String environment,
        int timeoutThresholdMs
    ) {
        this.id = id;
        this.projectId = projectId;
        this.name = name;
        this.baseUrl = baseUrl;
        this.healthEndpoint = healthEndpoint;
        this.environment = environment;
        this.timeoutThresholdMs = timeoutThresholdMs;
        this.status = ServiceStatus.HEALTHY;
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

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getHealthEndpoint() {
        return healthEndpoint;
    }

    public String getEnvironment() {
        return environment;
    }

    public int getTimeoutThresholdMs() {
        return timeoutThresholdMs;
    }

    public ServiceStatus getStatus() {
        return status;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setHealthEndpoint(String healthEndpoint) {
        this.healthEndpoint = healthEndpoint;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public void setTimeoutThresholdMs(int timeoutThresholdMs) {
        this.timeoutThresholdMs = timeoutThresholdMs;
    }

    public void setStatus(ServiceStatus status) {
        this.status = status;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("projectId", projectId);
        map.put("name", name);
        map.put("baseUrl", baseUrl);
        map.put("healthEndpoint", healthEndpoint);
        map.put("environment", environment);
        map.put("timeoutThresholdMs", timeoutThresholdMs);
        map.put("status", status.name());
        return map;
    }
}
