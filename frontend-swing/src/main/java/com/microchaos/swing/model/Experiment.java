package com.microchaos.swing.model;

public class Experiment {
    public long id;
    public String name;
    public long targetServiceId;
    public String targetServiceName;
    public String faultType;
    public String stressType;
    public int intensity;
    public int durationSeconds;

    @Override
    public String toString() {
        return "#" + id + " " + name + " -> " + (targetServiceName != null ? targetServiceName : targetServiceId);
    }
}
