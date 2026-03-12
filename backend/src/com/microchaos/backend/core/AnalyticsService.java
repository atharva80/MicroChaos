package com.microchaos.backend.core;

import com.microchaos.backend.model.ExperimentRun;
import com.microchaos.backend.model.MetricSnapshot;
import com.microchaos.backend.model.RunStatus;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AnalyticsService {
    private final InMemoryStore store;
    private final ExperimentService experimentService;
    private final MonitoringService monitoringService;

    public AnalyticsService(InMemoryStore store, ExperimentService experimentService, MonitoringService monitoringService) {
        this.store = store;
        this.experimentService = experimentService;
        this.monitoringService = monitoringService;
    }

    public Map<String, Object> dashboardOverview() {
        int totalServices = store.services().size();
        int totalExperiments = store.experiments().size();
        long activeRuns = store.runs().values().stream().filter(run -> run.getStatus() == RunStatus.RUNNING).count();
        long failedRuns = store.runs().values().stream().filter(run -> run.getStatus() == RunStatus.FAILED).count();
        double avgResilience = store.runs().values().stream().mapToDouble(ExperimentRun::getResilienceScore).average().orElse(0.0);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalServices", totalServices);
        response.put("totalExperiments", totalExperiments);
        response.put("activeRuns", activeRuns);
        response.put("failedRuns", failedRuns);
        response.put("averageResilienceScore", round(avgResilience));
        response.put("recentRuns", store.runs().values().stream().sorted((a, b) -> Long.compare(b.getId(), a.getId())).limit(5).map(ExperimentRun::toMap).toList());
        response.put("monitoring", monitoringService.monitoringOverview());
        return response;
    }

    public Map<String, Object> scorecard(long runId) {
        ExperimentRun run = experimentService.getRun(runId);
        List<MetricSnapshot> metrics = experimentService.metrics(runId);

        double avgError = metrics.stream().mapToDouble(MetricSnapshot::getErrorRate).average().orElse(0.0);
        double avgAvailability = metrics.stream().mapToDouble(MetricSnapshot::getAvailabilityPercent).average().orElse(100.0);
        double avgP95 = metrics.stream().mapToDouble(MetricSnapshot::getP95LatencyMs).average().orElse(0.0);

        double faultTolerance = scoreFromLowerBetter(avgError, 0.8);
        double recoverySpeed = scoreFromLowerBetter(run.getMttrSeconds(), 1.0);
        double observability = scoreFromLowerBetter(avgP95 / 10.0, 0.6);
        double dependencyStability = scoreFromHigherBetter(avgAvailability, 1.0);
        double remediationEffectiveness = scoreFromHigherBetter(run.getResilienceScore(), 1.0);

        Map<String, Object> scorecard = new LinkedHashMap<>();
        scorecard.put("runId", runId);
        scorecard.put("faultToleranceScore", round(faultTolerance));
        scorecard.put("recoverySpeedScore", round(recoverySpeed));
        scorecard.put("observabilityScore", round(observability));
        scorecard.put("dependencyStabilityScore", round(dependencyStability));
        scorecard.put("remediationEffectivenessScore", round(remediationEffectiveness));
        scorecard.put("overallScore", round((faultTolerance + recoverySpeed + observability + dependencyStability + remediationEffectiveness) / 5.0));
        return scorecard;
    }

    private static double scoreFromLowerBetter(double metric, double penaltyMultiplier) {
        return Math.max(0.0, 100.0 - metric * penaltyMultiplier);
    }

    private static double scoreFromHigherBetter(double metric, double multiplier) {
        return Math.max(0.0, Math.min(100.0, metric * multiplier));
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
