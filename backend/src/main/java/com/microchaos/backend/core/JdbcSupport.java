package com.microchaos.backend.core;

import java.sql.Connection;
import java.sql.SQLException;

final class JdbcSupport {
    private JdbcSupport() {}

    static Connection openConnection() throws SQLException {
        DatabaseConnection databaseConnection = new DatabaseConnection();
        databaseConnection.connect();
        Connection connection = databaseConnection.getConnection();
        if (connection == null) {
            throw new SQLException("Database connection is not available");
        }
        return connection;
    }
}
