package com.microchaos.backend.core;

import com.microchaos.backend.model.Experiment;
import com.microchaos.backend.model.ExperimentRun;
import com.microchaos.backend.model.MetricSnapshot;
import com.microchaos.backend.model.RemediationExecution;
import com.microchaos.backend.model.RemediationPolicy;
import com.microchaos.backend.model.RemediationRule;
import com.microchaos.backend.model.ServiceDependency;
import com.microchaos.backend.model.TargetService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryStore {
    private final AtomicLong ids = new AtomicLong(1000);

    private final Map<Long, TargetService> services = new ConcurrentHashMap<>();
    private final Map<Long, ServiceDependency> dependencies = new ConcurrentHashMap<>();
    private final Map<Long, Experiment> experiments = new ConcurrentHashMap<>();
    private final Map<Long, ExperimentRun> runs = new ConcurrentHashMap<>();
    private final Map<Long, List<MetricSnapshot>> metricsByRun = new ConcurrentHashMap<>();
    private final Map<Long, RemediationPolicy> remediationPolicies = new ConcurrentHashMap<>();
    private final Map<Long, RemediationRule> remediationRules = new ConcurrentHashMap<>();
    private final Map<Long, List<RemediationExecution>> remediationByRun = new ConcurrentHashMap<>();

    public long nextId() {
        return ids.incrementAndGet();
    }

    public Map<Long, TargetService> services() {
        return services;
    }

    public Map<Long, ServiceDependency> dependencies() {
        return dependencies;
    }

    public Map<Long, Experiment> experiments() {
        return experiments;
    }

    public Map<Long, ExperimentRun> runs() {
        return runs;
    }

    public Map<Long, List<MetricSnapshot>> metricsByRun() {
        return metricsByRun;
    }

    public Map<Long, RemediationPolicy> remediationPolicies() {
        return remediationPolicies;
    }

    public Map<Long, RemediationRule> remediationRules() {
        return remediationRules;
    }

    public Map<Long, List<RemediationExecution>> remediationByRun() {
        return remediationByRun;
    }

    public List<MetricSnapshot> metricsForRun(long runId) {
        return metricsByRun.computeIfAbsent(runId, ignored -> new ArrayList<>());
    }

    public List<RemediationExecution> remediationForRun(long runId) {
        return remediationByRun.computeIfAbsent(runId, ignored -> new ArrayList<>());
    }
}
