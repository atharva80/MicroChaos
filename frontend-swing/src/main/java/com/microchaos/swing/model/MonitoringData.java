package com.microchaos.swing.model;

public class MonitoringData {
    public long id;
    public long serviceId;
    public String serviceName;
    public String baseUrl;
    public String timestamp;
    public String monitoringState;
    public int healthStatusCode;
    public long healthResponseTimeMs;
    public boolean faultActive;
    public int activeFaultCount;
    public int latencyMs;
    public int timeoutMs;
    public double injectedErrorRate;
    public double authDenyRate;
    public boolean unavailable;
    public boolean fallbackEnabled;
    public int trafficReductionPercent;
    public long totalRequests;
    public long failedRequests;
    public String operationalStatus;
    public String details;

    @Override
    public String toString() {
        return serviceName + " - " + monitoringState;
    }
}
