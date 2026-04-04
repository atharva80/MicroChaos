package com.microchaos.swing.model;

public class Service {
    public long id;
    public String name;
    public String baseUrl;
    public String healthEndpoint;
    public String environment;
    public int timeoutThresholdMs;
    public String status;

    @Override
    public String toString() {
        return name + " (" + environment + ")";
    }
}
