package com.microchaos.backend.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class RemediationRule implements JsonEntity {
    private final long id;
    private final long policyId;
    private final String metricName;
    private final String operator;
    private final double thresholdValue;
    private final RemediationActionType actionType;
    private final int priority;

    public RemediationRule(
        long id,
        long policyId,
        String metricName,
        String operator,
        double thresholdValue,
        RemediationActionType actionType,
        int priority
    ) {
        this.id = id;
        this.policyId = policyId;
        this.metricName = metricName;
        this.operator = operator;
        this.thresholdValue = thresholdValue;
        this.actionType = actionType;
        this.priority = priority;
    }

    public long getId() {
        return id;
    }

    public long getPolicyId() {
        return policyId;
    }

    public String getMetricName() {
        return metricName;
    }

    public String getOperator() {
        return operator;
    }

    public double getThresholdValue() {
        return thresholdValue;
    }

    public RemediationActionType getActionType() {
        return actionType;
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("policyId", policyId);
        map.put("metricName", metricName);
        map.put("operator", operator);
        map.put("thresholdValue", thresholdValue);
        map.put("actionType", actionType.name());
        map.put("priority", priority);
        return map;
    }
}
