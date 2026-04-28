package com.microchaos.backend.model;

import java.util.Map;

public interface JsonEntity {
    Map<String, Object> toMap();
}
