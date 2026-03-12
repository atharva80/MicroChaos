package com.microchaos.backend.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class MetricSnapshot implements JsonEntity {
    private final long id;
    private final long runId;
    private final Instant timestamp;
    private final double responseTimeMs;
    private final double errorRate;
    private final double throughput;
    private final double p95LatencyMs;
    private final double availabilityPercent;

    public MetricSnapshot(
        long id,
        long runId,
        Instant timestamp,
        double responseTimeMs,
        double errorRate,
        double throughput,
        double p95LatencyMs,
        double availabilityPercent
    ) {
        this.id = id;
        this.runId = runId;
        this.timestamp = timestamp;
        this.responseTimeMs = responseTimeMs;
        this.errorRate = errorRate;
        this.throughput = throughput;
        this.p95LatencyMs = p95LatencyMs;
        this.availabilityPercent = availabilityPercent;
    }

    public long getRunId() {
        return runId;
    }

    public double getErrorRate() {
        return errorRate;
    }

    public double getAvailabilityPercent() {
        return availabilityPercent;
    }

    public double getResponseTimeMs() {
        return responseTimeMs;
    }

    public double getP95LatencyMs() {
        return p95LatencyMs;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("runId", runId);
        map.put("timestamp", timestamp.toString());
        map.put("responseTimeMs", responseTimeMs);
        map.put("errorRate", errorRate);
        map.put("throughput", throughput);
        map.put("p95LatencyMs", p95LatencyMs);
        map.put("availabilityPercent", availabilityPercent);
        return map;
    }
}
