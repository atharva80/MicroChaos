package com.microchaos.backend.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class ServiceObservation implements JsonEntity {
    private final long id;
    private final long serviceId;
    private final String serviceName;
    private final String baseUrl;
    private final Instant timestamp;
    private final MonitoringState monitoringState;
    private final int healthStatusCode;
    private final long healthResponseTimeMs;
    private final boolean faultActive;
    private final int activeFaultCount;
    private final int latencyMs;
    private final int timeoutMs;
    private final double injectedErrorRate;
    private final double authDenyRate;
    private final boolean unavailable;
    private final boolean fallbackEnabled;
    private final int trafficReductionPercent;
    private final long totalRequests;
    private final long failedRequests;
    private final String operationalStatus;
    private final String details;

    public ServiceObservation(
        long id,
        long serviceId,
        String serviceName,
        String baseUrl,
        Instant timestamp,
        MonitoringState monitoringState,
        int healthStatusCode,
        long healthResponseTimeMs,
        boolean faultActive,
        int activeFaultCount,
        int latencyMs,
        int timeoutMs,
        double injectedErrorRate,
        double authDenyRate,
        boolean unavailable,
        boolean fallbackEnabled,
        int trafficReductionPercent,
        long totalRequests,
        long failedRequests,
        String operationalStatus,
        String details
    ) {
        this.id = id;
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.baseUrl = baseUrl;
        this.timestamp = timestamp;
        this.monitoringState = monitoringState;
        this.healthStatusCode = healthStatusCode;
        this.healthResponseTimeMs = healthResponseTimeMs;
        this.faultActive = faultActive;
        this.activeFaultCount = activeFaultCount;
        this.latencyMs = latencyMs;
        this.timeoutMs = timeoutMs;
        this.injectedErrorRate = injectedErrorRate;
        this.authDenyRate = authDenyRate;
        this.unavailable = unavailable;
        this.fallbackEnabled = fallbackEnabled;
        this.trafficReductionPercent = trafficReductionPercent;
        this.totalRequests = totalRequests;
        this.failedRequests = failedRequests;
        this.operationalStatus = operationalStatus;
        this.details = details;
    }

    public long getServiceId() {
        return serviceId;
    }

    public MonitoringState getMonitoringState() {
        return monitoringState;
    }

    public boolean isFaultActive() {
        return faultActive;
    }

    public long getHealthResponseTimeMs() {
        return healthResponseTimeMs;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("serviceId", serviceId);
        map.put("serviceName", serviceName);
        map.put("baseUrl", baseUrl);
        map.put("timestamp", timestamp.toString());
        map.put("monitoringState", monitoringState.name());
        map.put("healthStatusCode", healthStatusCode);
        map.put("healthResponseTimeMs", healthResponseTimeMs);
        map.put("faultActive", faultActive);
        map.put("activeFaultCount", activeFaultCount);
        map.put("latencyMs", latencyMs);
        map.put("timeoutMs", timeoutMs);
        map.put("injectedErrorRate", injectedErrorRate);
        map.put("authDenyRate", authDenyRate);
        map.put("unavailable", unavailable);
        map.put("fallbackEnabled", fallbackEnabled);
        map.put("trafficReductionPercent", trafficReductionPercent);
        map.put("totalRequests", totalRequests);
        map.put("failedRequests", failedRequests);
        map.put("operationalStatus", operationalStatus);
        map.put("details", details);
        return map;
    }
}
