package com.microchaos.backend.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class ServiceDependency implements JsonEntity {
    private final long id;
    private final long sourceServiceId;
    private final long targetServiceId;
    private DependencyType dependencyType;
    private String protocol;
    private CommunicationMode communicationMode;
    private Criticality criticality;
    private boolean fallbackAvailable;

    public ServiceDependency(
        long id,
        long sourceServiceId,
        long targetServiceId,
        DependencyType dependencyType,
        String protocol,
        CommunicationMode communicationMode,
        Criticality criticality,
        boolean fallbackAvailable
    ) {
        this.id = id;
        this.sourceServiceId = sourceServiceId;
        this.targetServiceId = targetServiceId;
        this.dependencyType = dependencyType;
        this.protocol = protocol;
        this.communicationMode = communicationMode;
        this.criticality = criticality;
        this.fallbackAvailable = fallbackAvailable;
    }

    public long getId() {
        return id;
    }

    public long getSourceServiceId() {
        return sourceServiceId;
    }

    public long getTargetServiceId() {
        return targetServiceId;
    }

    public DependencyType getDependencyType() {
        return dependencyType;
    }

    public String getProtocol() {
        return protocol;
    }

    public CommunicationMode getCommunicationMode() {
        return communicationMode;
    }

    public Criticality getCriticality() {
        return criticality;
    }

    public boolean isFallbackAvailable() {
        return fallbackAvailable;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("sourceServiceId", sourceServiceId);
        map.put("targetServiceId", targetServiceId);
        map.put("dependencyType", dependencyType.name());
        map.put("protocol", protocol);
        map.put("communicationMode", communicationMode.name());
        map.put("criticality", criticality.name());
        map.put("fallbackAvailable", fallbackAvailable);
        return map;
    }
}
