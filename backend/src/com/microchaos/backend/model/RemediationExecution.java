package com.microchaos.backend.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class RemediationExecution implements JsonEntity {
    private final long id;
    private final long runId;
    private final long ruleId;
    private final RemediationActionType actionType;
    private String status;
    private final Instant startedAt;
    private Instant endedAt;
    private String resultSummary;

    public RemediationExecution(long id, long runId, long ruleId, RemediationActionType actionType) {
        this.id = id;
        this.runId = runId;
        this.ruleId = ruleId;
        this.actionType = actionType;
        this.status = "RUNNING";
        this.startedAt = Instant.now();
        this.resultSummary = "";
    }

    public long getRunId() {
        return runId;
    }

    public void complete(String status, String resultSummary) {
        this.status = status;
        this.resultSummary = resultSummary;
        this.endedAt = Instant.now();
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("runId", runId);
        map.put("ruleId", ruleId);
        map.put("actionType", actionType.name());
        map.put("status", status);
        map.put("startedAt", startedAt.toString());
        map.put("endedAt", endedAt == null ? null : endedAt.toString());
        map.put("resultSummary", resultSummary);
        return map;
    }
}
