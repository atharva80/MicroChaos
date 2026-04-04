package com.microchaos.swing.model;

public class DashboardOverview {
    public int totalServices;
    public int totalExperiments;
    public int activeRuns;
    public double averageResilienceScore;
    
    public static class Monitoring {
        public int healthyCount;
        public int degradedCount;
        public int downCount;
    }
    
    public Monitoring monitoring;
}
