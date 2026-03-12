package com.microchaos.backend.core;

import com.microchaos.backend.model.TargetService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class ServiceRegistryService {
    private final InMemoryStore store;

    public ServiceRegistryService(InMemoryStore store) {
        this.store = store;
    }

    public TargetService create(Map<String, String> params) {
        long id = store.nextId();
        long projectId = parseLong(params.get("projectId"), 1L);
        String name = required(params, "name");
        String baseUrl = required(params, "baseUrl");
        String healthEndpoint = params.getOrDefault("healthEndpoint", "/health");
        String environment = params.getOrDefault("environment", "dev");
        int timeoutMs = parseInt(params.get("timeoutThresholdMs"), 2000);

        TargetService service = new TargetService(
            id,
            projectId,
            name,
            baseUrl,
            healthEndpoint,
            environment,
            timeoutMs
        );
        store.services().put(id, service);
        return service;
    }

    public TargetService update(long id, Map<String, String> params) {
        TargetService service = get(id);
        if (params.containsKey("name")) {
            service.setName(params.get("name"));
        }
        if (params.containsKey("baseUrl")) {
            service.setBaseUrl(params.get("baseUrl"));
        }
        if (params.containsKey("healthEndpoint")) {
            service.setHealthEndpoint(params.get("healthEndpoint"));
        }
        if (params.containsKey("environment")) {
            service.setEnvironment(params.get("environment"));
        }
        if (params.containsKey("timeoutThresholdMs")) {
            service.setTimeoutThresholdMs(parseInt(params.get("timeoutThresholdMs"), service.getTimeoutThresholdMs()));
        }
        return service;
    }

    public TargetService get(long id) {
        TargetService service = store.services().get(id);
        if (service == null) {
            throw new IllegalArgumentException("Service not found: " + id);
        }
        return service;
    }

    public List<TargetService> list() {
        List<TargetService> services = new ArrayList<>(store.services().values());
        services.sort(Comparator.comparingLong(TargetService::getId));
        return services;
    }

    public boolean delete(long id) {
        return store.services().remove(id) != null;
    }

    public void seedDemoServices() {
        seedDemoServices(9000);
    }

    public void seedDemoServices(int basePort) {
        if (!store.services().isEmpty()) {
            return;
        }
        create(Map.of("name", "api-gateway", "baseUrl", "http://localhost:" + basePort));
        create(Map.of("name", "order-service", "baseUrl", "http://localhost:" + (basePort + 1)));
        create(Map.of("name", "payment-service", "baseUrl", "http://localhost:" + (basePort + 2)));
        create(Map.of("name", "inventory-service", "baseUrl", "http://localhost:" + (basePort + 3)));
        create(Map.of("name", "notification-service", "baseUrl", "http://localhost:" + (basePort + 4)));
        create(Map.of("name", "auth-service", "baseUrl", "http://localhost:" + (basePort + 5)));
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

    private static String required(Map<String, String> params, String key) {
        String value = params.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required param: " + key);
        }
        return value;
    }
}
