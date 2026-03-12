package com.microchaos.backend.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class ExperimentRun implements JsonEntity {
    private final long id;
    private final long experimentId;
    private final Instant startedAt;
    private Instant endedAt;
    private RunStatus status;
    private String summary;
    private long mttrSeconds;
    private double resilienceScore;

    public ExperimentRun(long id, long experimentId) {
        this.id = id;
        this.experimentId = experimentId;
        this.startedAt = Instant.now();
        this.status = RunStatus.RUNNING;
        this.summary = "";
    }

    public long getId() {
        return id;
    }

    public long getExperimentId() {
        return experimentId;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public RunStatus getStatus() {
        return status;
    }

    public String getSummary() {
        return summary;
    }

    public long getMttrSeconds() {
        return mttrSeconds;
    }

    public double getResilienceScore() {
        return resilienceScore;
    }

    public void complete(RunStatus status, String summary, long mttrSeconds, double resilienceScore) {
        this.status = status;
        this.summary = summary;
        this.mttrSeconds = mttrSeconds;
        this.resilienceScore = resilienceScore;
        this.endedAt = Instant.now();
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("experimentId", experimentId);
        map.put("startedAt", startedAt.toString());
        map.put("endedAt", endedAt == null ? null : endedAt.toString());
        map.put("status", status.name());
        map.put("summary", summary);
        map.put("mttrSeconds", mttrSeconds);
        map.put("resilienceScore", resilienceScore);
        return map;
    }
}
