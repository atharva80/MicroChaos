package com.microchaos.backend.api;

import com.microchaos.backend.core.AnalyticsService;
import com.microchaos.backend.core.ExperimentService;
import com.microchaos.backend.core.InMemoryStore;
import com.microchaos.backend.core.MonitoringService;
import com.microchaos.backend.core.ServiceRegistryService;
import com.microchaos.backend.core.ServiceControlService;
import com.microchaos.backend.core.TopologyService;
import com.microchaos.backend.model.Experiment;
import com.microchaos.backend.model.ExperimentRun;
import com.microchaos.backend.model.TargetService;
import com.microchaos.backend.util.HttpUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ApiRouter implements HttpHandler {
    private final InMemoryStore store;
    private final ServiceRegistryService serviceRegistryService;
    private final ServiceControlService serviceControlService;
    private final TopologyService topologyService;
    private final ExperimentService experimentService;
    private final AnalyticsService analyticsService;
    private final MonitoringService monitoringService;

    public ApiRouter(
        InMemoryStore store,
        ServiceRegistryService serviceRegistryService,
        ServiceControlService serviceControlService,
        TopologyService topologyService,
        ExperimentService experimentService,
        AnalyticsService analyticsService,
        MonitoringService monitoringService
    ) {
        this.store = store;
        this.serviceRegistryService = serviceRegistryService;
        this.serviceControlService = serviceControlService;
        this.topologyService = topologyService;
        this.experimentService = experimentService;
        this.analyticsService = analyticsService;
        this.monitoringService = monitoringService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                HttpUtil.sendNoContent(exchange, 204);
                return;
            }
            route(exchange);
        } catch (IllegalArgumentException ex) {
            HttpUtil.sendJson(exchange, 400, Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            HttpUtil.sendJson(exchange, 500, Map.of("error", "Internal server error", "details", ex.getMessage()));
        }
    }

    private void route(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if (path.equals("/api/health")) {
            HttpUtil.sendJson(exchange, 200, Map.of("status", "ok"));
            return;
        }
        if (path.equals("/api/services")) {
            handleServicesCollection(exchange, method);
            return;
        }
        if (path.startsWith("/api/services/") && path.endsWith("/faults/inject")) {
            handleServiceFaultInject(exchange, method, path);
            return;
        }
        if (path.startsWith("/api/services/") && path.endsWith("/faults/reset")) {
            handleServiceFaultReset(exchange, method, path);
            return;
        }
        if (path.startsWith("/api/services/")) {
            handleServiceItem(exchange, method, path);
            return;
        }
        if (path.equals("/api/topology/dependencies")) {
            handleDependencies(exchange, method);
            return;
        }
        if (path.equals("/api/topology/graph") && method.equals("GET")) {
            HttpUtil.sendJson(exchange, 200, topologyService.fullGraph());
            return;
        }
        if (path.startsWith("/api/topology/services/")) {
            handleTopologyServicePath(exchange, method, path);
            return;
        }
        if (path.startsWith("/api/topology/impact/") && method.equals("GET")) {
            long runId = parseTailId(path, "/api/topology/impact/");
            ExperimentRun run = experimentService.getRun(runId);
            Experiment experiment = store.experiments().get(run.getExperimentId());
            HttpUtil.sendJson(exchange, 200, topologyService.impactPath(experiment.getTargetServiceId()));
            return;
        }
        if (path.equals("/api/experiments")) {
            handleExperiments(exchange, method);
            return;
        }
        if (path.startsWith("/api/experiments/")) {
            handleExperimentPath(exchange, method, path);
            return;
        }
        if (path.equals("/api/runs") && method.equals("GET")) {
            HttpUtil.sendJson(exchange, 200, Map.of("items", experimentService.listRuns().stream().map(ExperimentRun::toMap).toList()));
            return;
        }
        if (path.startsWith("/api/runs/")) {
            handleRunsPath(exchange, method, path);
            return;
        }
        if (path.equals("/api/dashboard/overview") && method.equals("GET")) {
            HttpUtil.sendJson(exchange, 200, analyticsService.dashboardOverview());
            return;
        }
        if (path.equals("/api/monitoring/overview") && method.equals("GET")) {
            HttpUtil.sendJson(exchange, 200, monitoringService.monitoringOverview());
            return;
        }
        if (path.equals("/api/monitoring/services") && method.equals("GET")) {
            HttpUtil.sendJson(
                exchange,
                200,
                Map.of("items", monitoringService.latestStatuses().stream().map(item -> item.toMap()).toList())
            );
            return;
        }
        if (path.startsWith("/api/monitoring/services/") && path.endsWith("/history") && method.equals("GET")) {
            long serviceId = parseTailId(path, "/api/monitoring/services/", "/history");
            Integer limit = HttpUtil.parseInt(HttpUtil.queryParams(exchange).get("limit"), 40);
            HttpUtil.sendJson(
                exchange,
                200,
                Map.of("items", monitoringService.history(serviceId, limit).stream().map(item -> item.toMap()).toList())
            );
            return;
        }

        HttpUtil.sendJson(exchange, 404, Map.of("error", "Route not found"));
    }

    private void handleServiceFaultInject(HttpExchange exchange, String method, String path) throws IOException {
        if (!method.equals("POST")) {
            HttpUtil.sendJson(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }
        long serviceId = parseTailId(path, "/api/services/", "/faults/inject");
        Map<String, String> params = HttpUtil.queryParams(exchange);
        String type = params.getOrDefault("type", "LATENCY");
        int intensity = HttpUtil.parseInt(params.get("intensity"), 50);
        int durationSeconds = HttpUtil.parseInt(params.get("durationSeconds"), 60);
        HttpUtil.sendJson(exchange, 200, serviceControlService.injectFault(serviceId, type, intensity, durationSeconds));
    }

    private void handleServiceFaultReset(HttpExchange exchange, String method, String path) throws IOException {
        if (!method.equals("POST")) {
            HttpUtil.sendJson(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }
        long serviceId = parseTailId(path, "/api/services/", "/faults/reset");
        HttpUtil.sendJson(exchange, 200, serviceControlService.resetFaults(serviceId));
    }

    private void handleServicesCollection(HttpExchange exchange, String method) throws IOException {
        if (method.equals("GET")) {
            List<Map<String, Object>> items = serviceRegistryService.list().stream().map(TargetService::toMap).toList();
            HttpUtil.sendJson(exchange, 200, Map.of("items", items));
            return;
        }
        if (method.equals("POST")) {
            Map<String, String> params = HttpUtil.queryParams(exchange);
            TargetService created = serviceRegistryService.create(params);
            HttpUtil.sendJson(exchange, 201, created.toMap());
            return;
        }
        HttpUtil.sendJson(exchange, 405, Map.of("error", "Method not allowed"));
    }

    private void handleServiceItem(HttpExchange exchange, String method, String path) throws IOException {
        long id = parseTailId(path, "/api/services/");
        if (method.equals("GET")) {
            HttpUtil.sendJson(exchange, 200, serviceRegistryService.get(id).toMap());
            return;
        }
        if (method.equals("PUT")) {
            HttpUtil.sendJson(exchange, 200, serviceRegistryService.update(id, HttpUtil.queryParams(exchange)).toMap());
            return;
        }
        if (method.equals("DELETE")) {
            boolean deleted = serviceRegistryService.delete(id);
            HttpUtil.sendJson(exchange, 200, Map.of("deleted", deleted));
            return;
        }
        HttpUtil.sendJson(exchange, 405, Map.of("error", "Method not allowed"));
    }

    private void handleDependencies(HttpExchange exchange, String method) throws IOException {
        if (method.equals("GET")) {
            HttpUtil.sendJson(
                exchange,
                200,
                Map.of("items", topologyService.listDependencies().stream().map(dep -> dep.toMap()).toList())
            );
            return;
        }
        if (method.equals("POST")) {
            HttpUtil.sendJson(exchange, 201, topologyService.createDependency(HttpUtil.queryParams(exchange)).toMap());
            return;
        }
        HttpUtil.sendJson(exchange, 405, Map.of("error", "Method not allowed"));
    }

    private void handleTopologyServicePath(HttpExchange exchange, String method, String path) throws IOException {
        if (!method.equals("GET")) {
            HttpUtil.sendJson(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }
        String[] parts = path.split("/");
        if (parts.length != 6) {
            HttpUtil.sendJson(exchange, 404, Map.of("error", "Invalid topology path"));
            return;
        }
        long serviceId = Long.parseLong(parts[4]);
        String direction = parts[5];
        if ("upstream".equals(direction)) {
            HttpUtil.sendJson(exchange, 200, Map.of("items", topologyService.upstreamOf(serviceId)));
            return;
        }
        if ("downstream".equals(direction)) {
            HttpUtil.sendJson(exchange, 200, Map.of("items", topologyService.downstreamOf(serviceId)));
            return;
        }
        HttpUtil.sendJson(exchange, 404, Map.of("error", "Invalid topology direction"));
    }

    private void handleExperiments(HttpExchange exchange, String method) throws IOException {
        if (method.equals("GET")) {
            HttpUtil.sendJson(exchange, 200, Map.of("items", experimentService.listExperiments().stream().map(Experiment::toMap).toList()));
            return;
        }
        if (method.equals("POST")) {
            HttpUtil.sendJson(exchange, 201, experimentService.create(HttpUtil.queryParams(exchange)).toMap());
            return;
        }
        HttpUtil.sendJson(exchange, 405, Map.of("error", "Method not allowed"));
    }

    private void handleExperimentPath(HttpExchange exchange, String method, String path) throws IOException {
        if (path.endsWith("/run") && method.equals("POST")) {
            long experimentId = parseTailId(path, "/api/experiments/", "/run");
            HttpUtil.sendJson(exchange, 200, experimentService.runExperiment(experimentId));
            return;
        }
        if (path.endsWith("/stop") && method.equals("POST")) {
            long experimentId = parseTailId(path, "/api/experiments/", "/stop");
            HttpUtil.sendJson(exchange, 200, experimentService.stopExperiment(experimentId));
            return;
        }
        long experimentId = parseTailId(path, "/api/experiments/");
        if (method.equals("GET")) {
            HttpUtil.sendJson(exchange, 200, experimentService.getExperiment(experimentId).toMap());
            return;
        }
        HttpUtil.sendJson(exchange, 405, Map.of("error", "Method not allowed"));
    }

    private void handleRunsPath(HttpExchange exchange, String method, String path) throws IOException {
        String[] parts = path.split("/");
        if (parts.length < 4) {
            HttpUtil.sendJson(exchange, 404, Map.of("error", "Invalid run path"));
            return;
        }
        long runId = Long.parseLong(parts[3]);
        if (parts.length == 4 && method.equals("GET")) {
            HttpUtil.sendJson(exchange, 200, experimentService.getRun(runId).toMap());
            return;
        }
        if (parts.length == 5 && "metrics".equals(parts[4]) && method.equals("GET")) {
            HttpUtil.sendJson(exchange, 200, Map.of("items", experimentService.metrics(runId).stream().map(metric -> metric.toMap()).toList()));
            return;
        }
        if (parts.length == 5 && "scorecard".equals(parts[4]) && method.equals("GET")) {
            HttpUtil.sendJson(exchange, 200, analyticsService.scorecard(runId));
            return;
        }
        HttpUtil.sendJson(exchange, 404, Map.of("error", "Unknown run route"));
    }

    private static long parseTailId(String path, String prefix) {
        return Long.parseLong(path.substring(prefix.length()));
    }

    private static long parseTailId(String path, String prefix, String suffix) {
        return Long.parseLong(path.substring(prefix.length(), path.length() - suffix.length()));
    }
}
