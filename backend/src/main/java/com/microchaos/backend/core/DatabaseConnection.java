package com.microchaos.backend.core;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String DB_HOST = env("DB_HOST", "localhost");
    private static final String DB_PORT = env("DB_PORT", "5432");
    private static final String DB_NAME = env("DB_NAME", "microchaos");
    private static final String DB_USER = env("DB_USER", "microchaos");
    private static final String DB_PASSWORD = env("DB_PASSWORD", "microchaos");
    private static final String DB_URL = "jdbc:postgresql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME;

    private Connection connection;

    public void connect() {
        try {
            // Load PostgreSQL driver
            Class.forName("org.postgresql.Driver");

            // Establish connection
            this.connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

            System.out.println("[DB] Connected to PostgreSQL successfully.");
        } catch (ClassNotFoundException e) {
            System.err.println("[DB] Driver not found: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("[DB] Connection failed: " + e.getMessage());
        }
    }

    public Connection getConnection() {
        return this.connection;
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("[DB] Connection closed.");
            } catch (SQLException e) {
                System.err.println("[DB] Error closing connection: " + e.getMessage());
            }
        }
    }

    public boolean isConnected() {
        return connection != null;
    }

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
