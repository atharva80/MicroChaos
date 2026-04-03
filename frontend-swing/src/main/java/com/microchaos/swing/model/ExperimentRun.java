package com.microchaos.swing.model;

public class ExperimentRun {
    public long id;
    public long experimentId;
    public String status;
    public double resilienceScore;
    public long mttrSeconds;
    public String startedAt;
    public String endedAt;

    @Override
    public String toString() {
        return "Run #" + id + " - Status: " + status + " - Score: " + resilienceScore;
    }
}
