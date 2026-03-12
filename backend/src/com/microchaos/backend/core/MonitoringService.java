package com.microchaos.backend.core;

import com.microchaos.backend.model.MonitoringState;
import com.microchaos.backend.model.ServiceObservation;
import com.microchaos.backend.model.TargetService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MonitoringService {
    private static final int MAX_HISTORY_PER_SERVICE = 120;

    private static final Pattern STRING_PATTERN_TEMPLATE = Pattern.compile("\"PLACEHOLDER\":\"([^\"]*)\"");
    private static final Pattern NUMBER_PATTERN_TEMPLATE = Pattern.compile("\"PLACEHOLDER\":(-?\\d+(?:\\.\\d+)?)");
    private static final Pattern BOOLEAN_PATTERN_TEMPLATE = Pattern.compile("\"PLACEHOLDER\":(true|false)");

    private final InMemoryStore store;
    private final HttpClient httpClient;
    private final ScheduledExecutorService scheduler;
    private final Map<Long, Deque<ServiceObservation>> historyByService;

    public MonitoringService(InMemoryStore store) {
        this.store = store;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.historyByService = new ConcurrentHashMap<>();
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::pollAllServices, 0, 3, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdownNow();
    }

    public List<ServiceObservation> latestStatuses() {
        List<ServiceObservation> latest = new ArrayList<>();
        for (TargetService service : store.services().values()) {
            Deque<ServiceObservation> history = historyByService.get(service.getId());
            if (history == null || history.isEmpty()) {
                latest.add(unknownObservation(service));
                continue;
            }
            latest.add(history.getLast());
        }
        latest.sort(Comparator.comparingLong(ServiceObservation::getServiceId));
        return latest;
    }

    public List<ServiceObservation> history(long serviceId, int limit) {
        Deque<ServiceObservation> history = historyByService.get(serviceId);
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        int bounded = Math.max(1, Math.min(limit, MAX_HISTORY_PER_SERVICE));
        List<ServiceObservation> items = new ArrayList<>(history);
        int from = Math.max(0, items.size() - bounded);
        return items.subList(from, items.size());
    }

    public Map<String, Object> monitoringOverview() {
        List<ServiceObservation> latest = latestStatuses();
        long healthy = latest.stream().filter(item -> item.getMonitoringState() == MonitoringState.HEALTHY).count();
        long degraded = latest.stream().filter(item -> item.getMonitoringState() == MonitoringState.DEGRADED).count();
        long down = latest.stream().filter(item -> item.getMonitoringState() == MonitoringState.DOWN).count();
        long unknown = latest.stream().filter(item -> item.getMonitoringState() == MonitoringState.UNKNOWN).count();
        long faulted = latest.stream().filter(ServiceObservation::isFaultActive).count();
        double avgHealthLatency = latest.stream().mapToLong(ServiceObservation::getHealthResponseTimeMs).average().orElse(0.0);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("healthyCount", healthy);
        map.put("degradedCount", degraded);
        map.put("downCount", down);
        map.put("unknownCount", unknown);
        map.put("faultedServicesCount", faulted);
        map.put("averageHealthLatencyMs", round(avgHealthLatency));
        map.put("services", latest.stream().map(ServiceObservation::toMap).toList());
        return map;
    }

    private void pollAllServices() {
        for (TargetService service : store.services().values()) {
            ServiceObservation observation = pollService(service);
            Deque<ServiceObservation> deque = historyByService.computeIfAbsent(service.getId(), ignored -> new ArrayDeque<>());
            deque.addLast(observation);
            while (deque.size() > MAX_HISTORY_PER_SERVICE) {
                deque.removeFirst();
            }
        }
    }

    private ServiceObservation pollService(TargetService service) {
        Instant now = Instant.now();
        long startNanos = System.nanoTime();

        int healthCode = 0;
        String healthBody = "";
        try {
            HttpResponse<String> healthResponse = httpClient.send(
                HttpRequest.newBuilder(URI.create(service.getBaseUrl() + service.getHealthEndpoint())).timeout(Duration.ofSeconds(2)).GET().build(),
                HttpResponse.BodyHandlers.ofString()
            );
            healthCode = healthResponse.statusCode();
            healthBody = healthResponse.body();
        } catch (Exception ex) {
            long responseMs = elapsedMillis(startNanos);
            return new ServiceObservation(
                store.nextId(),
                service.getId(),
                service.getName(),
                service.getBaseUrl(),
                now,
                MonitoringState.DOWN,
                0,
                responseMs,
                false,
                0,
                0,
                0,
                0.0,
                0.0,
                false,
                false,
                0,
                0,
                0,
                "DOWN",
                "Health endpoint unreachable: " + ex.getClass().getSimpleName()
            );
        }
        long healthResponseMs = elapsedMillis(startNanos);

        String statusBody = fetchStatusBody(service.getBaseUrl());
        int latencyMs = extractInt(statusBody, "latencyMs", extractInt(healthBody, "latencyMs", 0));
        int timeoutMs = extractInt(statusBody, "timeoutMs", extractInt(healthBody, "timeoutMs", 0));
        double injectedErrorRate = extractDouble(statusBody, "errorRatePercent", 0.0);
        double authDenyRate = extractDouble(statusBody, "authDenyRatePercent", 0.0);
        boolean unavailable = extractBoolean(statusBody, "unavailable", false);
        boolean fallbackEnabled = extractBoolean(statusBody, "fallbackEnabled", false);
        int trafficReduction = extractInt(statusBody, "trafficReductionPercent", 0);
        long totalRequests = extractLong(statusBody, "totalRequests", 0L);
        long failedRequests = extractLong(statusBody, "failedRequests", 0L);

        String operationalStatus = extractString(statusBody, "operationalStatus", extractString(healthBody, "status", "UNKNOWN"));
        int activeFaultCount = countActiveFaults(latencyMs, timeoutMs, injectedErrorRate, authDenyRate, unavailable);
        boolean faultActive = activeFaultCount > 0;
        MonitoringState state = resolveState(healthCode, operationalStatus, unavailable);

        return new ServiceObservation(
            store.nextId(),
            service.getId(),
            service.getName(),
            service.getBaseUrl(),
            now,
            state,
            healthCode,
            healthResponseMs,
            faultActive,
            activeFaultCount,
            latencyMs,
            timeoutMs,
            round(injectedErrorRate),
            round(authDenyRate),
            unavailable,
            fallbackEnabled,
            trafficReduction,
            totalRequests,
            failedRequests,
            operationalStatus,
            statusBody.isBlank() ? healthBody : statusBody
        );
    }

    private static MonitoringState resolveState(int healthCode, String operationalStatus, boolean unavailable) {
        if (unavailable || healthCode >= 500 || "DOWN".equalsIgnoreCase(operationalStatus)) {
            return MonitoringState.DOWN;
        }
        if ("DEGRADED".equalsIgnoreCase(operationalStatus)) {
            return MonitoringState.DEGRADED;
        }
        if (healthCode > 0 && healthCode < 500) {
            return MonitoringState.HEALTHY;
        }
        return MonitoringState.UNKNOWN;
    }

    private static int countActiveFaults(int latencyMs, int timeoutMs, double errorRate, double authDenyRate, boolean unavailable) {
        int count = 0;
        if (latencyMs > 0) {
            count++;
        }
        if (timeoutMs > 0) {
            count++;
        }
        if (errorRate > 0.0) {
            count++;
        }
        if (authDenyRate > 0.0) {
            count++;
        }
        if (unavailable) {
            count++;
        }
        return count;
    }

    private String fetchStatusBody(String baseUrl) {
        try {
            HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/admin/status")).timeout(Duration.ofSeconds(2)).GET().build(),
                HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() >= 200 && response.statusCode() < 500) {
                return response.body();
            }
            return "";
        } catch (Exception ex) {
            return "";
        }
    }

    private ServiceObservation unknownObservation(TargetService service) {
        return new ServiceObservation(
            store.nextId(),
            service.getId(),
            service.getName(),
            service.getBaseUrl(),
            Instant.now(),
            MonitoringState.UNKNOWN,
            0,
            0,
            false,
            0,
            0,
            0,
            0.0,
            0.0,
            false,
            false,
            0,
            0,
            0,
            "UNKNOWN",
            "No observation yet"
        );
    }

    private static String extractString(String json, String key, String fallback) {
        if (json == null || json.isBlank()) {
            return fallback;
        }
        Pattern pattern = Pattern.compile(STRING_PATTERN_TEMPLATE.pattern().replace("PLACEHOLDER", Pattern.quote(key)));
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : fallback;
    }

    private static int extractInt(String json, String key, int fallback) {
        Double number = extractNumber(json, key);
        if (number == null) {
            return fallback;
        }
        return number.intValue();
    }

    private static long extractLong(String json, String key, long fallback) {
        Double number = extractNumber(json, key);
        if (number == null) {
            return fallback;
        }
        return number.longValue();
    }

    private static double extractDouble(String json, String key, double fallback) {
        Double number = extractNumber(json, key);
        return number == null ? fallback : number;
    }

    private static Double extractNumber(String json, String key) {
        if (json == null || json.isBlank()) {
            return null;
        }
        Pattern pattern = Pattern.compile(NUMBER_PATTERN_TEMPLATE.pattern().replace("PLACEHOLDER", Pattern.quote(key)));
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Double.parseDouble(matcher.group(1));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static boolean extractBoolean(String json, String key, boolean fallback) {
        if (json == null || json.isBlank()) {
            return fallback;
        }
        Pattern pattern = Pattern.compile(BOOLEAN_PATTERN_TEMPLATE.pattern().replace("PLACEHOLDER", Pattern.quote(key)));
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            return fallback;
        }
        return Boolean.parseBoolean(matcher.group(1));
    }

    private static long elapsedMillis(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
