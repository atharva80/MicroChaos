package com.microchaos.backend.core;

import com.microchaos.backend.model.Experiment;
import com.microchaos.backend.model.ExperimentRun;
import com.microchaos.backend.model.MetricSnapshot;
import com.microchaos.backend.model.RemediationActionType;
import com.microchaos.backend.model.RemediationExecution;
import com.microchaos.backend.model.RemediationMode;
import com.microchaos.backend.model.RemediationPolicy;
import com.microchaos.backend.model.RemediationRule;
import com.microchaos.backend.model.RunStatus;
import com.microchaos.backend.model.TargetService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RemediationService {
    private final InMemoryStore store;
    private final ServiceRegistryService serviceRegistry;
    private final ExperimentService experimentService;
    private final HttpClient httpClient;

    public RemediationService(InMemoryStore store, ServiceRegistryService serviceRegistry, ExperimentService experimentService) {
        this.store = store;
        this.serviceRegistry = serviceRegistry;
        this.experimentService = experimentService;
        this.httpClient = HttpClient.newHttpClient();
    }

    public RemediationPolicy createPolicy(Map<String, String> params) {
        RemediationPolicy policy = new RemediationPolicy(
            store.nextId(),
            parseLong(params.get("projectId"), 1L),
            required(params, "name"),
            RemediationMode.from(params.get("mode")),
            Boolean.parseBoolean(params.getOrDefault("isActive", "true"))
        );
        store.remediationPolicies().put(policy.getId(), policy);
        return policy;
    }

    public List<RemediationPolicy> listPolicies() {
        List<RemediationPolicy> policies = new ArrayList<>(store.remediationPolicies().values());
        policies.sort(Comparator.comparingLong(RemediationPolicy::getId));
        return policies;
    }

    public RemediationRule createRule(Map<String, String> params) {
        long policyId = parseRequiredLong(params, "policyId");
        if (!store.remediationPolicies().containsKey(policyId)) {
            throw new IllegalArgumentException("Policy not found: " + policyId);
        }
        RemediationRule rule = new RemediationRule(
            store.nextId(),
            policyId,
            params.getOrDefault("metricName", "errorRate"),
            params.getOrDefault("operator", ">="),
            parseDouble(params.get("thresholdValue"), 25.0),
            RemediationActionType.from(params.get("actionType")),
            parseInt(params.get("priority"), 10)
        );
        store.remediationRules().put(rule.getId(), rule);
        return rule;
    }

    public List<RemediationRule> listRules(long policyId) {
        List<RemediationRule> rules = store
            .remediationRules()
            .values()
            .stream()
            .filter(rule -> rule.getPolicyId() == policyId)
            .sorted(Comparator.comparingInt(RemediationRule::getPriority))
            .toList();
        return new ArrayList<>(rules);
    }

    public Map<String, Object> remediateRun(long runId, long policyId) {
        ExperimentRun run = experimentService.getRun(runId);
        RemediationPolicy policy = store.remediationPolicies().get(policyId);
        if (policy == null) {
            throw new IllegalArgumentException("Policy not found: " + policyId);
        }
        List<RemediationRule> rules = listRules(policyId);
        if (rules.isEmpty()) {
            throw new IllegalArgumentException("No rules found for policy " + policyId);
        }

        Map<String, Double> aggregates = aggregateRunMetrics(experimentService.metrics(runId));
        Experiment experiment = store.experiments().get(run.getExperimentId());
        TargetService targetService = serviceRegistry.get(experiment.getTargetServiceId());

        List<Map<String, Object>> executionResults = new ArrayList<>();
        for (RemediationRule rule : rules) {
            double actualValue = aggregates.getOrDefault(rule.getMetricName(), 0.0);
            if (!matches(rule.getOperator(), actualValue, rule.getThresholdValue())) {
                continue;
            }
            RemediationExecution execution = new RemediationExecution(
                store.nextId(),
                runId,
                rule.getId(),
                rule.getActionType()
            );
            String result = executeAction(rule.getActionType(), targetService, run);
            execution.complete("COMPLETED", result);
            store.remediationForRun(runId).add(execution);
            executionResults.add(execution.toMap());

            if (rule.getActionType() == RemediationActionType.STOP_EXPERIMENT) {
                run.complete(RunStatus.STOPPED, run.getSummary() + " | Remediation stopped run", run.getMttrSeconds(), run.getResilienceScore());
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("policy", policy.toMap());
        response.put("triggeredActions", executionResults);
        response.put("aggregates", aggregates);
        response.put("triggeredCount", executionResults.size());
        return response;
    }

    public List<RemediationExecution> runHistory(long runId) {
        return new ArrayList<>(store.remediationForRun(runId));
    }

    private String executeAction(RemediationActionType actionType, TargetService targetService, ExperimentRun run) {
        return switch (actionType) {
            case ENABLE_FALLBACK -> callAndSummarize(targetService.getBaseUrl() + "/admin/fallback/enable", "fallback enabled");
            case RESTART_SERVICE -> callAndSummarize(targetService.getBaseUrl() + "/admin/restart", "restart requested");
            case REDUCE_TRAFFIC -> callAndSummarize(targetService.getBaseUrl() + "/admin/traffic/reduce", "traffic reduced");
            case STOP_EXPERIMENT -> "run " + run.getId() + " marked for stop";
        };
    }

    private String callAndSummarize(String url, String fallbackMessage) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).POST(HttpRequest.BodyPublishers.noBody()).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return "status=" + response.statusCode();
        } catch (Exception ex) {
            return fallbackMessage + " (simulated)";
        }
    }

    private static Map<String, Double> aggregateRunMetrics(List<MetricSnapshot> metrics) {
        Map<String, Double> result = new LinkedHashMap<>();
        double errorRate = metrics.stream().mapToDouble(MetricSnapshot::getErrorRate).average().orElse(0.0);
        double availability = metrics.stream().mapToDouble(MetricSnapshot::getAvailabilityPercent).average().orElse(100.0);
        double responseTime = metrics.stream().mapToDouble(MetricSnapshot::getResponseTimeMs).average().orElse(0.0);
        double p95 = metrics.stream().mapToDouble(MetricSnapshot::getP95LatencyMs).average().orElse(0.0);

        result.put("errorRate", round(errorRate));
        result.put("availability", round(availability));
        result.put("responseTimeMs", round(responseTime));
        result.put("p95LatencyMs", round(p95));
        return result;
    }

    private static boolean matches(String operator, double actual, double threshold) {
        return switch (operator.trim()) {
            case ">" -> actual > threshold;
            case ">=" -> actual >= threshold;
            case "<" -> actual < threshold;
            case "<=" -> actual <= threshold;
            case "==" -> actual == threshold;
            default -> false;
        };
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static long parseLong(String raw, long defaultValue) {
        try {
            return raw == null ? defaultValue : Long.parseLong(raw);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static int parseInt(String raw, int defaultValue) {
        try {
            return raw == null ? defaultValue : Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static double parseDouble(String raw, double defaultValue) {
        try {
            return raw == null ? defaultValue : Double.parseDouble(raw);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static long parseRequiredLong(Map<String, String> params, String key) {
        String raw = params.get(key);
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Missing required param: " + key);
        }
        return Long.parseLong(raw);
    }

    private static String required(Map<String, String> params, String key) {
        String value = params.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required param: " + key);
        }
        return value;
    }
}
