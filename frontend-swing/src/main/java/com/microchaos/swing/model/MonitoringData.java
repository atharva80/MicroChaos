package com.microchaos.swing.model;

public class MonitoringData {
    public long serviceId;
    public String serviceName;
    public String status;
    public double responseTimeMs;
    public double errorRate;
    public double throughput;
    public double availabilityPercent;

    @Override
    public String toString() {
        return serviceName + " - " + status;
    }
}
