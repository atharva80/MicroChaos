package com.microchaos.backend;

import com.microchaos.backend.api.ApiRouter;
import com.microchaos.backend.core.AnalyticsService;
import com.microchaos.backend.core.ExperimentService;
import com.microchaos.backend.core.InMemoryStore;
import com.microchaos.backend.core.MonitoringService;
import com.microchaos.backend.core.ServiceControlService;
import com.microchaos.backend.core.ServiceRegistryService;
import com.microchaos.backend.core.TopologyService;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class MicroChaosBackendApplication {
    public static void main(String[] args) throws Exception {
        int port = parsePort(System.getenv().getOrDefault("PORT", "8080"));
        int demoBasePort = parsePort(System.getenv().getOrDefault("DEMO_BASE_PORT", "9000"));

        InMemoryStore store = new InMemoryStore();
        ServiceRegistryService serviceRegistryService = new ServiceRegistryService(store);
        ServiceControlService serviceControlService = new ServiceControlService(serviceRegistryService);
        TopologyService topologyService = new TopologyService(store, serviceRegistryService);
        ExperimentService experimentService = new ExperimentService(store, serviceRegistryService, topologyService);
        MonitoringService monitoringService = new MonitoringService(store);
        AnalyticsService analyticsService = new AnalyticsService(store, experimentService, monitoringService);

        seedDemoData(serviceRegistryService, topologyService, demoBasePort);

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext(
            "/api",
            new ApiRouter(store, serviceRegistryService, serviceControlService, topologyService, experimentService, analyticsService, monitoringService)
        );
        server.setExecutor(Executors.newFixedThreadPool(12));
        monitoringService.start();
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(monitoringService::stop));

        System.out.println("MicroChaos backend running on http://localhost:" + port);
    }

    private static void seedDemoData(ServiceRegistryService serviceRegistryService, TopologyService topologyService, int demoBasePort) {
        serviceRegistryService.seedDemoServices(demoBasePort);
        topologyService.seedDemoDependencies();
    }

    private static int parsePort(String rawPort) {
        try {
            return Integer.parseInt(rawPort);
        } catch (NumberFormatException ex) {
            return 8080;
        }
    }
}
