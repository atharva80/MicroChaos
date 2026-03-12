package com.microchaos.backend.core;

import com.microchaos.backend.model.CommunicationMode;
import com.microchaos.backend.model.Criticality;
import com.microchaos.backend.model.DependencyType;
import com.microchaos.backend.model.ServiceDependency;
import com.microchaos.backend.model.TargetService;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class TopologyService {
    private final InMemoryStore store;
    private final ServiceRegistryService serviceRegistry;

    public TopologyService(InMemoryStore store, ServiceRegistryService serviceRegistry) {
        this.store = store;
        this.serviceRegistry = serviceRegistry;
    }

    public ServiceDependency createDependency(Map<String, String> params) {
        long sourceServiceId = parseRequiredLong(params, "sourceServiceId");
        long targetServiceId = parseRequiredLong(params, "targetServiceId");
        serviceRegistry.get(sourceServiceId);
        serviceRegistry.get(targetServiceId);

        ServiceDependency dependency = new ServiceDependency(
            store.nextId(),
            sourceServiceId,
            targetServiceId,
            DependencyType.from(params.get("dependencyType")),
            params.getOrDefault("protocol", "HTTP"),
            CommunicationMode.from(params.get("communicationMode")),
            Criticality.from(params.get("criticality")),
            Boolean.parseBoolean(params.getOrDefault("fallbackAvailable", "false"))
        );
        store.dependencies().put(dependency.getId(), dependency);
        return dependency;
    }

    public List<ServiceDependency> listDependencies() {
        List<ServiceDependency> dependencies = new ArrayList<>(store.dependencies().values());
        dependencies.sort(Comparator.comparingLong(ServiceDependency::getId));
        return dependencies;
    }

    public Map<String, Object> fullGraph() {
        List<Map<String, Object>> nodes = serviceRegistry.list().stream().map(TargetService::toMap).toList();
        List<Map<String, Object>> edges = listDependencies().stream().map(ServiceDependency::toMap).toList();
        Map<String, Object> graph = new LinkedHashMap<>();
        graph.put("nodes", nodes);
        graph.put("edges", edges);
        return graph;
    }

    public List<Map<String, Object>> upstreamOf(long serviceId) {
        Set<Long> visited = new HashSet<>();
        Queue<Long> queue = new ArrayDeque<>();
        queue.add(serviceId);
        visited.add(serviceId);
        List<Map<String, Object>> results = new ArrayList<>();

        while (!queue.isEmpty()) {
            long current = queue.poll();
            for (ServiceDependency dependency : store.dependencies().values()) {
                if (dependency.getTargetServiceId() == current && !visited.contains(dependency.getSourceServiceId())) {
                    visited.add(dependency.getSourceServiceId());
                    queue.add(dependency.getSourceServiceId());
                    results.add(linkWithNames(dependency));
                }
            }
        }
        return results;
    }

    public List<Map<String, Object>> downstreamOf(long serviceId) {
        Set<Long> visited = new HashSet<>();
        Queue<Long> queue = new ArrayDeque<>();
        queue.add(serviceId);
        visited.add(serviceId);
        List<Map<String, Object>> results = new ArrayList<>();

        while (!queue.isEmpty()) {
            long current = queue.poll();
            for (ServiceDependency dependency : store.dependencies().values()) {
                if (dependency.getSourceServiceId() == current && !visited.contains(dependency.getTargetServiceId())) {
                    visited.add(dependency.getTargetServiceId());
                    queue.add(dependency.getTargetServiceId());
                    results.add(linkWithNames(dependency));
                }
            }
        }
        return results;
    }

    public Map<String, Object> impactPath(long targetServiceId) {
        List<Map<String, Object>> downstream = downstreamOf(targetServiceId);
        Map<String, Object> impact = new LinkedHashMap<>();
        impact.put("faultedServiceId", targetServiceId);
        impact.put("impactEdges", downstream);
        impact.put("impactedServicesCount", downstream.size());
        return impact;
    }

    public void seedDemoDependencies() {
        if (!store.dependencies().isEmpty()) {
            return;
        }
        long gateway = findServiceIdByName("api-gateway");
        long order = findServiceIdByName("order-service");
        long payment = findServiceIdByName("payment-service");
        long inventory = findServiceIdByName("inventory-service");
        long notification = findServiceIdByName("notification-service");
        long auth = findServiceIdByName("auth-service");

        createDependency(
            Map.of(
                "sourceServiceId",
                String.valueOf(gateway),
                "targetServiceId",
                String.valueOf(order),
                "dependencyType",
                "REST",
                "protocol",
                "HTTP",
                "communicationMode",
                "SYNC",
                "criticality",
                "CRITICAL",
                "fallbackAvailable",
                "false"
            )
        );
        createDependency(
            Map.of(
                "sourceServiceId",
                String.valueOf(gateway),
                "targetServiceId",
                String.valueOf(auth),
                "dependencyType",
                "AUTH",
                "protocol",
                "HTTP",
                "communicationMode",
                "SYNC",
                "criticality",
                "HIGH",
                "fallbackAvailable",
                "false"
            )
        );
        createDependency(
            Map.of(
                "sourceServiceId",
                String.valueOf(order),
                "targetServiceId",
                String.valueOf(payment),
                "dependencyType",
                "REST",
                "protocol",
                "HTTP",
                "communicationMode",
                "SYNC",
                "criticality",
                "CRITICAL",
                "fallbackAvailable",
                "true"
            )
        );
        createDependency(
            Map.of(
                "sourceServiceId",
                String.valueOf(order),
                "targetServiceId",
                String.valueOf(inventory),
                "dependencyType",
                "REST",
                "protocol",
                "HTTP",
                "communicationMode",
                "SYNC",
                "criticality",
                "CRITICAL",
                "fallbackAvailable",
                "true"
            )
        );
        createDependency(
            Map.of(
                "sourceServiceId",
                String.valueOf(order),
                "targetServiceId",
                String.valueOf(notification),
                "dependencyType",
                "QUEUE",
                "protocol",
                "EVENT",
                "communicationMode",
                "ASYNC",
                "criticality",
                "MEDIUM",
                "fallbackAvailable",
                "true"
            )
        );
    }

    private Map<String, Object> linkWithNames(ServiceDependency dependency) {
        Map<String, Object> map = new LinkedHashMap<>(dependency.toMap());
        TargetService source = serviceRegistry.get(dependency.getSourceServiceId());
        TargetService target = serviceRegistry.get(dependency.getTargetServiceId());
        map.put("sourceName", source.getName());
        map.put("targetName", target.getName());
        return map;
    }

    private long findServiceIdByName(String name) {
        return serviceRegistry
            .list()
            .stream()
            .filter(service -> service.getName().equals(name))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Missing seeded service: " + name))
            .getId();
    }

    private static long parseRequiredLong(Map<String, String> params, String key) {
        String raw = params.get(key);
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Missing required param: " + key);
        }
        return Long.parseLong(raw);
    }
}
