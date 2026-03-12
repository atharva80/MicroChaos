package com.microchaos.backend.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class Experiment implements JsonEntity {
    private final long id;
    private final long projectId;
    private String name;
    private String description;
    private long targetServiceId;
    private FaultType faultType;
    private StressType stressType;
    private int durationSeconds;
    private int intensity;
    private RemediationMode remediationMode;
    private int blastRadiusLimit;
    private ExperimentStatus status;
    private final long createdBy;
    private final Instant createdAt;

    public Experiment(
        long id,
        long projectId,
        String name,
        String description,
        long targetServiceId,
        FaultType faultType,
        StressType stressType,
        int durationSeconds,
        int intensity,
        RemediationMode remediationMode,
        int blastRadiusLimit,
        long createdBy
    ) {
        this.id = id;
        this.projectId = projectId;
        this.name = name;
        this.description = description;
        this.targetServiceId = targetServiceId;
        this.faultType = faultType;
        this.stressType = stressType;
        this.durationSeconds = durationSeconds;
        this.intensity = intensity;
        this.remediationMode = remediationMode;
        this.blastRadiusLimit = blastRadiusLimit;
        this.status = ExperimentStatus.DRAFT;
        this.createdBy = createdBy;
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

    public String getDescription() {
        return description;
    }

    public long getTargetServiceId() {
        return targetServiceId;
    }

    public FaultType getFaultType() {
        return faultType;
    }

    public StressType getStressType() {
        return stressType;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public int getIntensity() {
        return intensity;
    }

    public RemediationMode getRemediationMode() {
        return remediationMode;
    }

    public int getBlastRadiusLimit() {
        return blastRadiusLimit;
    }

    public ExperimentStatus getStatus() {
        return status;
    }

    public void setStatus(ExperimentStatus status) {
        this.status = status;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("projectId", projectId);
        map.put("name", name);
        map.put("description", description);
        map.put("targetServiceId", targetServiceId);
        map.put("faultType", faultType.name());
        map.put("stressType", stressType.name());
        map.put("durationSeconds", durationSeconds);
        map.put("intensity", intensity);
        map.put("remediationMode", remediationMode.name());
        map.put("blastRadiusLimit", blastRadiusLimit);
        map.put("status", status.name());
        map.put("createdBy", createdBy);
        map.put("createdAt", createdAt.toString());
        return map;
    }
}
