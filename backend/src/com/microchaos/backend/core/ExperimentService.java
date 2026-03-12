package com.microchaos.backend.core;

import com.microchaos.backend.model.Experiment;
import com.microchaos.backend.model.ExperimentRun;
import com.microchaos.backend.model.ExperimentStatus;
import com.microchaos.backend.model.FaultType;
import com.microchaos.backend.model.MetricSnapshot;
import com.microchaos.backend.model.RemediationMode;
import com.microchaos.backend.model.RunStatus;
import com.microchaos.backend.model.StressType;
import com.microchaos.backend.model.TargetService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ExperimentService {
    private final InMemoryStore store;
    private final ServiceRegistryService serviceRegistry;
    private final TopologyService topologyService;
    private final HttpClient httpClient;
    private final Random random;

    public ExperimentService(InMemoryStore store, ServiceRegistryService serviceRegistry, TopologyService topologyService) {
        this.store = store;
        this.serviceRegistry = serviceRegistry;
        this.topologyService = topologyService;
        this.httpClient = HttpClient.newHttpClient();
        this.random = new Random();
    }

    public Experiment create(Map<String, String> params) {
        long targetServiceId = parseRequiredLong(params, "targetServiceId");
        serviceRegistry.get(targetServiceId);

        Experiment experiment = new Experiment(
            store.nextId(),
            parseLong(params.get("projectId"), 1L),
            required(params, "name"),
            params.getOrDefault("description", ""),
            targetServiceId,
            FaultType.from(params.get("faultType")),
            StressType.from(params.get("stressType")),
            parseInt(params.get("durationSeconds"), 60),
            parseInt(params.get("intensity"), 30),
            RemediationMode.from(params.get("remediationMode")),
            parseInt(params.get("blastRadiusLimit"), 3),
            parseLong(params.get("createdBy"), 1L)
        );
        store.experiments().put(experiment.getId(), experiment);
        return experiment;
    }

    public List<Experiment> listExperiments() {
        List<Experiment> experiments = new ArrayList<>(store.experiments().values());
        experiments.sort(Comparator.comparingLong(Experiment::getId));
        return experiments;
    }

    public Experiment getExperiment(long id) {
        Experiment experiment = store.experiments().get(id);
        if (experiment == null) {
            throw new IllegalArgumentException("Experiment not found: " + id);
        }
        return experiment;
    }

    public Map<String, Object> runExperiment(long experimentId) {
        Experiment experiment = getExperiment(experimentId);
        TargetService targetService = serviceRegistry.get(experiment.getTargetServiceId());

        experiment.setStatus(ExperimentStatus.RUNNING);
        ExperimentRun run = new ExperimentRun(store.nextId(), experimentId);
        store.runs().put(run.getId(), run);

        String faultResult = applyFault(targetService, experiment);
        List<MetricSnapshot> metrics = simulateMetrics(run.getId(), experiment, targetService);
        store.metricsByRun().put(run.getId(), metrics);

        long mttr = estimateMttr(metrics, experiment);
        double score = computeResilienceScore(metrics, mttr);
        run.complete(
            RunStatus.COMPLETED,
            "Executed fault " + experiment.getFaultType().name() + " on " + targetService.getName() + ". " + faultResult,
            mttr,
            score
        );
        experiment.setStatus(ExperimentStatus.COMPLETED);
        clearFault(targetService);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("run", run.toMap());
        response.put("impact", topologyService.impactPath(targetService.getId()));
        response.put("metricsCount", metrics.size());
        return response;
    }

    public Map<String, Object> stopExperiment(long experimentId) {
        Experiment experiment = getExperiment(experimentId);
        ExperimentRun run = latestRunForExperiment(experimentId);
        if (run == null || run.getStatus() != RunStatus.RUNNING) {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("message", "No running experiment found for id " + experimentId);
            return response;
        }
        TargetService targetService = serviceRegistry.get(experiment.getTargetServiceId());
        clearFault(targetService);
        run.complete(RunStatus.STOPPED, "Experiment manually stopped", 0, run.getResilienceScore());
        experiment.setStatus(ExperimentStatus.STOPPED);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("run", run.toMap());
        response.put("message", "Experiment stopped");
        return response;
    }

    public ExperimentRun getRun(long runId) {
        ExperimentRun run = store.runs().get(runId);
        if (run == null) {
            throw new IllegalArgumentException("Run not found: " + runId);
        }
        return run;
    }

    public List<ExperimentRun> listRuns() {
        List<ExperimentRun> runs = new ArrayList<>(store.runs().values());
        runs.sort(Comparator.comparingLong(ExperimentRun::getId));
        return runs;
    }

    public List<MetricSnapshot> metrics(long runId) {
        return new ArrayList<>(store.metricsForRun(runId));
    }

    private ExperimentRun latestRunForExperiment(long experimentId) {
        return store
            .runs()
            .values()
            .stream()
            .filter(run -> run.getExperimentId() == experimentId)
            .max(Comparator.comparingLong(ExperimentRun::getId))
            .orElse(null);
    }

    private String applyFault(TargetService service, Experiment experiment) {
        try {
            URI uri = URI.create(
                service.getBaseUrl() +
                "/faults/configure?type=" +
                experiment.getFaultType().name() +
                "&intensity=" +
                experiment.getIntensity() +
                "&durationSeconds=" +
                experiment.getDurationSeconds()
            );
            HttpRequest request = HttpRequest.newBuilder(uri).POST(HttpRequest.BodyPublishers.noBody()).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return "Fault endpoint status=" + response.statusCode();
        } catch (Exception ex) {
            return "Fault endpoint unreachable, simulated only";
        }
    }

    private void clearFault(TargetService service) {
        try {
            URI uri = URI.create(service.getBaseUrl() + "/faults/reset");
            HttpRequest request = HttpRequest.newBuilder(uri).POST(HttpRequest.BodyPublishers.noBody()).build();
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {
            // If demo services are not running, the run remains a simulation.
        }
    }

    private List<MetricSnapshot> simulateMetrics(long runId, Experiment experiment, TargetService service) {
        List<MetricSnapshot> snapshots = new ArrayList<>();
        int samples = Math.max(8, experiment.getDurationSeconds() / 5);
        double baseLatency = service.getTimeoutThresholdMs() * 0.7;
        double faultMultiplier = 1.0 + (experiment.getIntensity() / 100.0) * 2.0;
        double errorBase = Math.min(5 + experiment.getIntensity() / 2.0, 60.0);

        for (int i = 0; i < samples; i++) {
            double noise = random.nextDouble() * 0.3 + 0.85;
            double responseTime = baseLatency * faultMultiplier * noise;
            double p95 = responseTime * (1.2 + random.nextDouble() * 0.25);
            double errorRate = Math.min(100.0, errorBase * noise);
            double throughput = Math.max(1.0, 250.0 / faultMultiplier * noise);
            double availability = Math.max(0.0, 100.0 - errorRate);

            MetricSnapshot snapshot = new MetricSnapshot(
                store.nextId(),
                runId,
                Instant.now().plusSeconds(i),
                round(responseTime),
                round(errorRate),
                round(throughput),
                round(p95),
                round(availability)
            );
            snapshots.add(snapshot);
        }
        return snapshots;
    }

    private static long estimateMttr(List<MetricSnapshot> metrics, Experiment experiment) {
        double avgError = metrics.stream().mapToDouble(MetricSnapshot::getErrorRate).average().orElse(0.0);
        long base = Math.max(10, experiment.getDurationSeconds() / 3);
        return Math.round(base + avgError / 4.0);
    }

    private static double computeResilienceScore(List<MetricSnapshot> metrics, long mttrSeconds) {
        double avgError = metrics.stream().mapToDouble(MetricSnapshot::getErrorRate).average().orElse(0.0);
        double avgAvailability = metrics.stream().mapToDouble(MetricSnapshot::getAvailabilityPercent).average().orElse(100.0);
        double penalty = avgError * 0.45 + mttrSeconds * 0.3 + (100.0 - avgAvailability) * 0.5;
        return round(Math.max(0.0, 100.0 - penalty));
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
