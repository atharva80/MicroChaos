package com.microchaos.backend.core;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class DemoJdbcBootstrap {
    private final String host;
    private final int port;
    private final String database;
    private final String user;
    private final String password;
    private Connection connection;

    public DemoJdbcBootstrap() {
        this.host = env("DB_HOST", "localhost");
        this.port = parseInt(env("DB_PORT", "5432"), 5432);
        this.database = env("DB_NAME", "microchaos");
        this.user = env("DB_USER", "microchaos");
        this.password = env("DB_PASSWORD", "microchaos");
    }

    public String jdbcUrl() {
        return "jdbc:postgresql://" + host + ":" + port + "/" + database;
    }

    public Map<String, Object> metadata() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("driver", "org.postgresql.Driver");
        map.put("jdbcUrl", jdbcUrl());
        map.put("user", user);
        map.put("passwordConfigured", !password.isBlank());
        map.put("mode", connection != null ? "JDBC_CONNECTED" : "IN_MEMORY_DEMO");
        return map;
    }

    public void initialize() {
        System.out.println("[DB] Initializing PostgreSQL JDBC bootstrap");
        try {
            // Load PostgreSQL driver
            Class.forName("org.postgresql.Driver");
            
            // Establish real connection
            this.connection = DriverManager.getConnection(
                jdbcUrl(), user, password
            );
            
            System.out.println("[DB] Driver: org.postgresql.Driver");
            System.out.println("[DB] JDBC URL: " + jdbcUrl());
            System.out.println("[DB] User: " + user);
            System.out.println("[DB] Connection status: CONNECTED");
        } catch (ClassNotFoundException e) {
            System.err.println("[DB] ERROR: PostgreSQL driver not found on classpath");
            System.out.println("[DB] Falling back to IN_MEMORY_DEMO mode");
        } catch (SQLException e) {
            System.err.println("[DB] ERROR: Failed to connect - " + e.getMessage());
            System.out.println("[DB] Falling back to IN_MEMORY_DEMO mode");
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                System.err.println("[DB] Error closing connection: " + e.getMessage());
            }
        }
    }

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static int parseInt(String raw, int defaultValue) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
