package com.microchaos.demo;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;

public class DemoStackLauncher {
    private static final HttpClient HTTP_CLIENT = HttpClient
        .newBuilder()
        .connectTimeout(Duration.ofSeconds(2))
        .build();
    private static final Random RANDOM = new Random();

    public static void main(String[] args) throws Exception {
        int basePort = parseInt(System.getenv().get("DEMO_BASE_PORT"), 9000);

        ServiceNode gateway = new ServiceNode("api-gateway", basePort);
        ServiceNode order = new ServiceNode("order-service", basePort + 1);
        ServiceNode payment = new ServiceNode("payment-service", basePort + 2);
        ServiceNode inventory = new ServiceNode("inventory-service", basePort + 3);
        ServiceNode notification = new ServiceNode("notification-service", basePort + 4);
        ServiceNode auth = new ServiceNode("auth-service", basePort + 5);

        registerDefaultControlEndpoints(gateway);
        registerDefaultControlEndpoints(order);
        registerDefaultControlEndpoints(payment);
        registerDefaultControlEndpoints(inventory);
        registerDefaultControlEndpoints(notification);
        registerDefaultControlEndpoints(auth);

        registerBusinessEndpoints(gateway, order, payment, inventory, notification, auth);

        gateway.start();
        order.start();
        payment.start();
        inventory.start();
        notification.start();
        auth.start();

        System.out.println("Demo stack running:");
        System.out.println("api-gateway: http://localhost:" + gateway.port());
        System.out.println("order-service: http://localhost:" + order.port());
        System.out.println("payment-service: http://localhost:" + payment.port());
        System.out.println("inventory-service: http://localhost:" + inventory.port());
        System.out.println("notification-service: http://localhost:" + notification.port());
        System.out.println("auth-service: http://localhost:" + auth.port());
    }

    private static void registerBusinessEndpoints(
        ServiceNode gateway,
        ServiceNode order,
        ServiceNode payment,
        ServiceNode inventory,
        ServiceNode notification,
        ServiceNode auth
    ) {
        payment.register("/payments/charge", exchange -> {
            if (payment.beforeBusiness(exchange)) {
                return;
            }
            sendJson(
                exchange,
                200,
                mapOf("service", payment.name, "charged", true, "transactionId", "txn-" + System.currentTimeMillis())
            );
        });

        inventory.register("/inventory/reserve", exchange -> {
            if (inventory.beforeBusiness(exchange)) {
                return;
            }
            sendJson(exchange, 200, mapOf("service", inventory.name, "reserved", true));
        });

        notification.register("/notify", exchange -> {
            if (notification.beforeBusiness(exchange)) {
                return;
            }
            sendJson(exchange, 202, mapOf("service", notification.name, "status", "queued"));
        });

        auth.register("/auth/validate", exchange -> {
            if (auth.beforeBusiness(exchange)) {
                return;
            }
            boolean denied = RANDOM.nextDouble() * 100.0 < auth.faultProfile.authDenyRatePercent;
            if (denied) {
                sendJson(exchange, 401, mapOf("service", auth.name, "authorized", false));
                return;
            }
            sendJson(exchange, 200, mapOf("service", auth.name, "authorized", true));
        });

        order.register("/orders/create", exchange -> {
            if (order.beforeBusiness(exchange)) {
                return;
            }
            Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
            String userId = params.getOrDefault("userId", "1");
            String itemId = params.getOrDefault("itemId", "SKU-1");

            Response authResult = call(url(auth.port(), "/auth/validate?userId=" + userId));
            if (authResult.statusCode != 200) {
                sendJson(exchange, 401, mapOf("service", order.name, "status", "auth_failed", "authCode", authResult.statusCode));
                return;
            }

            Response paymentResult = call(url(payment.port(), "/payments/charge?userId=" + userId));
            if (paymentResult.statusCode != 200) {
                sendJson(
                    exchange,
                    502,
                    mapOf("service", order.name, "status", "payment_failed", "paymentCode", paymentResult.statusCode)
                );
                return;
            }

            Response inventoryResult = call(url(inventory.port(), "/inventory/reserve?itemId=" + itemId));
            if (inventoryResult.statusCode != 200) {
                if (order.fallbackEnabled) {
                    sendJson(
                        exchange,
                        200,
                        mapOf("service", order.name, "status", "completed_with_fallback", "inventoryFallback", true)
                    );
                } else {
                    sendJson(
                        exchange,
                        502,
                        mapOf("service", order.name, "status", "inventory_failed", "inventoryCode", inventoryResult.statusCode)
                    );
                }
                return;
            }

            call(url(notification.port(), "/notify?event=order_created"));
            sendJson(exchange, 200, mapOf("service", order.name, "status", "order_created"));
        });

        gateway.register("/api/checkout", exchange -> {
            if (gateway.beforeBusiness(exchange)) {
                return;
            }
            Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
            String userId = params.getOrDefault("userId", "1");
            String itemId = params.getOrDefault("itemId", "SKU-1");
            Response result = call(url(order.port(), "/orders/create?userId=" + userId + "&itemId=" + itemId));
            sendJson(exchange, result.statusCode, mapOf("service", gateway.name, "upstreamStatus", result.statusCode, "body", result.body));
        });
    }

    private static String url(int port, String path) {
        return "http://localhost:" + port + path;
    }

    private static void registerDefaultControlEndpoints(ServiceNode node) {
        node.register("/health", exchange -> {
            String status = node.operationalStatus();
            int statusCode = "DOWN".equals(status) ? 503 : 200;
            sendJson(exchange, statusCode, node.healthMap());
        });

        node.register("/faults/configure", exchange -> {
            Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
            node.configureFault(params);
            sendJson(exchange, 200, mapOf("service", node.name, "faultProfile", node.faultProfile.toMap()));
        });

        node.register("/faults/reset", exchange -> {
            node.resetFaults();
            sendJson(exchange, 200, mapOf("service", node.name, "faultProfile", node.faultProfile.toMap()));
        });

        node.register("/admin/restart", exchange -> {
            node.resetFaults();
            sendJson(exchange, 200, mapOf("service", node.name, "action", "restart_simulated"));
        });

        node.register("/admin/fallback/enable", exchange -> {
            node.fallbackEnabled = true;
            sendJson(exchange, 200, mapOf("service", node.name, "fallbackEnabled", true));
        });

        node.register("/admin/traffic/reduce", exchange -> {
            node.trafficReductionPercent = Math.min(90, node.trafficReductionPercent + 20);
            sendJson(exchange, 200, mapOf("service", node.name, "trafficReductionPercent", node.trafficReductionPercent));
        });

        node.register("/admin/status", exchange -> sendJson(exchange, 200, node.statusMap()));
    }

    private static Response call(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().timeout(Duration.ofSeconds(2)).build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            return new Response(response.statusCode(), response.body());
        } catch (Exception ex) {
            return new Response(503, "{\"error\":\"unreachable\"}");
        }
    }

    private static Map<String, Object> mapOf(Object... keyValuePairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            map.put(String.valueOf(keyValuePairs[i]), keyValuePairs[i + 1]);
        }
        return map;
    }

    private static void sendJson(HttpExchange exchange, int status, Map<String, Object> body) throws IOException {
        byte[] bytes = toJson(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String toJson(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String s) {
            return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Map<?, ?> map) {
            StringBuilder builder = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    builder.append(",");
                }
                first = false;
                builder.append(toJson(String.valueOf(entry.getKey()))).append(":").append(toJson(entry.getValue()));
            }
            return builder.append("}").toString();
        }
        return "\"" + String.valueOf(value) + "\"";
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> params = new HashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return params;
        }
        for (String pair : rawQuery.split("&")) {
            int idx = pair.indexOf('=');
            if (idx < 0) {
                params.put(decode(pair), "");
                continue;
            }
            params.put(decode(pair.substring(0, idx)), decode(pair.substring(idx + 1)));
        }
        return params;
    }

    private static String decode(String raw) {
        return URLDecoder.decode(raw, StandardCharsets.UTF_8);
    }

    private static int parseInt(String raw, int defaultValue) {
        try {
            return raw == null ? defaultValue : Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    @FunctionalInterface
    private interface RouteHandler {
        void handle(HttpExchange exchange) throws IOException;
    }

    private record Response(int statusCode, String body) {}

    private static class FaultProfile {
        int latencyMs;
        int timeoutMs;
        double errorRatePercent;
        double authDenyRatePercent;
        boolean unavailable;

        Map<String, Object> toMap() {
            return mapOf(
                "latencyMs",
                latencyMs,
                "timeoutMs",
                timeoutMs,
                "errorRatePercent",
                errorRatePercent,
                "authDenyRatePercent",
                authDenyRatePercent,
                "unavailable",
                unavailable
            );
        }
    }

    private static class ServiceNode {
        private final String name;
        private final int port;
        private final HttpServer server;
        private final FaultProfile faultProfile;
        private final long startedAtMs;
        private boolean fallbackEnabled;
        private int trafficReductionPercent;
        private String lastFaultType;
        private long totalRequests;
        private long failedRequests;

        ServiceNode(String name, int port) throws IOException {
            this.name = name;
            this.port = port;
            this.server = HttpServer.create(new InetSocketAddress(port), 0);
            this.server.setExecutor(Executors.newFixedThreadPool(8));
            this.faultProfile = new FaultProfile();
            this.startedAtMs = System.currentTimeMillis();
            this.fallbackEnabled = false;
            this.trafficReductionPercent = 0;
            this.lastFaultType = "NONE";
            this.totalRequests = 0;
            this.failedRequests = 0;
        }

        void register(String path, RouteHandler handler) {
            server.createContext(path, new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    if ("OPTIONS".equals(exchange.getRequestMethod())) {
                        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
                        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
                        exchange.sendResponseHeaders(204, -1);
                        exchange.close();
                        return;
                    }
                    handler.handle(exchange);
                }
            });
        }

        boolean beforeBusiness(HttpExchange exchange) throws IOException {
            totalRequests++;
            if (faultProfile.unavailable) {
                failedRequests++;
                sendJson(exchange, 503, mapOf("service", name, "error", "Service unavailable fault active"));
                return true;
            }
            if (faultProfile.timeoutMs > 0) {
                sleep(faultProfile.timeoutMs);
                failedRequests++;
                sendJson(exchange, 504, mapOf("service", name, "error", "Timeout fault active"));
                return true;
            }
            if (faultProfile.latencyMs > 0) {
                sleep(faultProfile.latencyMs);
            }
            if (trafficReductionPercent > 0 && RANDOM.nextDouble() * 100.0 < trafficReductionPercent) {
                failedRequests++;
                sendJson(exchange, 429, mapOf("service", name, "error", "Traffic reduced by remediation"));
                return true;
            }
            if (faultProfile.errorRatePercent > 0 && RANDOM.nextDouble() * 100.0 < faultProfile.errorRatePercent) {
                failedRequests++;
                sendJson(exchange, 500, mapOf("service", name, "error", "Injected random error"));
                return true;
            }
            return false;
        }

        void configureFault(Map<String, String> params) {
            String type = params.getOrDefault("type", "LATENCY").trim().toUpperCase();
            int intensity = parseInt(params.get("intensity"), 30);
            lastFaultType = type;
            switch (type) {
                case "LATENCY" -> faultProfile.latencyMs = Math.max(20, intensity * 15);
                case "ERROR_RESPONSE" -> faultProfile.errorRatePercent = Math.max(1.0, Math.min(95.0, intensity));
                case "TIMEOUT" -> faultProfile.timeoutMs = Math.max(200, intensity * 20);
                case "DATABASE_CONNECTION", "DEPENDENCY_UNAVAILABLE" -> faultProfile.unavailable = true;
                case "CPU_SPIKE", "THREAD_POOL_EXHAUSTION" -> {
                    faultProfile.latencyMs = Math.max(faultProfile.latencyMs, intensity * 10);
                    faultProfile.errorRatePercent = Math.max(faultProfile.errorRatePercent, intensity / 2.0);
                }
                case "AUTH_FAILURE" -> faultProfile.authDenyRatePercent = Math.max(5.0, Math.min(95.0, intensity));
                default -> faultProfile.latencyMs = Math.max(20, intensity * 10);
            }
        }

        void resetFaults() {
            faultProfile.latencyMs = 0;
            faultProfile.timeoutMs = 0;
            faultProfile.errorRatePercent = 0;
            faultProfile.authDenyRatePercent = 0;
            faultProfile.unavailable = false;
            fallbackEnabled = false;
            trafficReductionPercent = 0;
            lastFaultType = "NONE";
        }

        void start() {
            server.start();
        }

        int port() {
            return port;
        }

        String operationalStatus() {
            if (faultProfile.unavailable) {
                return "DOWN";
            }
            if (
                faultProfile.timeoutMs > 0 ||
                faultProfile.latencyMs > 0 ||
                faultProfile.errorRatePercent > 0 ||
                faultProfile.authDenyRatePercent > 0 ||
                trafficReductionPercent > 0
            ) {
                return "DEGRADED";
            }
            return "HEALTHY";
        }

        int activeFaultCount() {
            int count = 0;
            if (faultProfile.latencyMs > 0) {
                count++;
            }
            if (faultProfile.timeoutMs > 0) {
                count++;
            }
            if (faultProfile.errorRatePercent > 0) {
                count++;
            }
            if (faultProfile.authDenyRatePercent > 0) {
                count++;
            }
            if (faultProfile.unavailable) {
                count++;
            }
            return count;
        }

        double observedFailureRatePercent() {
            if (totalRequests == 0) {
                return 0.0;
            }
            return round((failedRequests * 100.0) / totalRequests);
        }

        Map<String, Object> healthMap() {
            return mapOf(
                "service",
                name,
                "status",
                operationalStatus(),
                "activeFaultCount",
                activeFaultCount(),
                "faultActive",
                activeFaultCount() > 0,
                "failureRatePercent",
                observedFailureRatePercent(),
                "uptimeMs",
                System.currentTimeMillis() - startedAtMs
            );
        }

        Map<String, Object> statusMap() {
            return mapOf(
                "service",
                name,
                "port",
                port,
                "operationalStatus",
                operationalStatus(),
                "activeFaultCount",
                activeFaultCount(),
                "lastFaultType",
                lastFaultType,
                "fallbackEnabled",
                fallbackEnabled,
                "trafficReductionPercent",
                trafficReductionPercent,
                "totalRequests",
                totalRequests,
                "failedRequests",
                failedRequests,
                "observedFailureRatePercent",
                observedFailureRatePercent(),
                "uptimeMs",
                System.currentTimeMillis() - startedAtMs,
                "latencyMs",
                faultProfile.latencyMs,
                "timeoutMs",
                faultProfile.timeoutMs,
                "errorRatePercent",
                faultProfile.errorRatePercent,
                "authDenyRatePercent",
                faultProfile.authDenyRatePercent,
                "unavailable",
                faultProfile.unavailable
            );
        }

        private static int parseInt(String raw, int defaultValue) {
            try {
                return raw == null ? defaultValue : Integer.parseInt(raw);
            } catch (NumberFormatException ex) {
                return defaultValue;
            }
        }

        private static void sleep(int millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }

        private static double round(double value) {
            return Math.round(value * 100.0) / 100.0;
        }
    }
}
