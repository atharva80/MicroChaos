package com.microchaos.backend.core;

import com.microchaos.backend.model.TargetService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public class ServiceControlService {
    private final ServiceRegistryService serviceRegistryService;
    private final HttpClient httpClient;

    public ServiceControlService(ServiceRegistryService serviceRegistryService) {
        this.serviceRegistryService = serviceRegistryService;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    }

    public Map<String, Object> injectFault(long serviceId, String type, int intensity, int durationSeconds) {
        TargetService service = serviceRegistryService.get(serviceId);
        String url =
            service.getBaseUrl() +
            "/faults/configure?type=" +
            type +
            "&intensity=" +
            intensity +
            "&durationSeconds=" +
            durationSeconds;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("serviceId", serviceId);
        response.put("serviceName", service.getName());
        response.put("requestedFaultType", type);
        response.put("intensity", intensity);
        response.put("durationSeconds", durationSeconds);

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).POST(HttpRequest.BodyPublishers.noBody()).build();
            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            response.put("status", httpResponse.statusCode());
            response.put("response", httpResponse.body());
            return response;
        } catch (Exception ex) {
            response.put("status", 503);
            response.put("error", "fault endpoint unreachable: " + ex.getClass().getSimpleName());
            return response;
        }
    }

    public Map<String, Object> resetFaults(long serviceId) {
        TargetService service = serviceRegistryService.get(serviceId);
        String url = service.getBaseUrl() + "/faults/reset";

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("serviceId", serviceId);
        response.put("serviceName", service.getName());

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).POST(HttpRequest.BodyPublishers.noBody()).build();
            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            response.put("status", httpResponse.statusCode());
            response.put("response", httpResponse.body());
            return response;
        } catch (Exception ex) {
            response.put("status", 503);
            response.put("error", "reset endpoint unreachable: " + ex.getClass().getSimpleName());
            return response;
        }
    }
}
