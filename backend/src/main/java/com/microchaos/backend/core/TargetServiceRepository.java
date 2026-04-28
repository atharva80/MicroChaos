package com.microchaos.backend.core;

import com.microchaos.backend.model.ServiceStatus;
import com.microchaos.backend.model.TargetService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TargetServiceRepository {
    private static final String INSERT_SQL =
        """
        INSERT INTO target_services (
            project_id, name, base_url, health_endpoint, environment, status, timeout_threshold_ms
        ) VALUES (?, ?, ?, ?, ?, ?, ?)
        RETURNING id
        """;

    private static final String UPDATE_SQL =
        """
        UPDATE target_services
        SET name = ?, base_url = ?, health_endpoint = ?, environment = ?, status = ?, timeout_threshold_ms = ?
        WHERE id = ?
        """;

    private static final String FIND_BY_ID_SQL =
        """
        SELECT id, project_id, name, base_url, health_endpoint, environment, status, timeout_threshold_ms
        FROM target_services
        WHERE id = ?
        """;

    private static final String FIND_ALL_SQL =
        """
        SELECT id, project_id, name, base_url, health_endpoint, environment, status, timeout_threshold_ms
        FROM target_services
        ORDER BY id
        """;

    private static final String DELETE_SQL = "DELETE FROM target_services WHERE id = ?";
    private static final String COUNT_SQL = "SELECT COUNT(*) FROM target_services";

    public TargetService create(TargetService service) {
        try (Connection connection = JdbcSupport.openConnection();
            PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            statement.setLong(1, service.getProjectId());
            statement.setString(2, service.getName());
            statement.setString(3, service.getBaseUrl());
            statement.setString(4, service.getHealthEndpoint());
            statement.setString(5, service.getEnvironment());
            statement.setString(6, service.getStatus().name());
            statement.setInt(7, service.getTimeoutThresholdMs());

            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException("Failed to insert target service");
                }
                return new TargetService(
                    rs.getLong("id"),
                    service.getProjectId(),
                    service.getName(),
                    service.getBaseUrl(),
                    service.getHealthEndpoint(),
                    service.getEnvironment(),
                    service.getTimeoutThresholdMs()
                );
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to create target service", ex);
        }
    }

    public TargetService update(TargetService service) {
        try (Connection connection = JdbcSupport.openConnection();
            PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            statement.setString(1, service.getName());
            statement.setString(2, service.getBaseUrl());
            statement.setString(3, service.getHealthEndpoint());
            statement.setString(4, service.getEnvironment());
            statement.setString(5, service.getStatus().name());
            statement.setInt(6, service.getTimeoutThresholdMs());
            statement.setLong(7, service.getId());

            int updated = statement.executeUpdate();
            if (updated == 0) {
                throw new IllegalArgumentException("Service not found: " + service.getId());
            }
            return service;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to update target service " + service.getId(), ex);
        }
    }

    public Optional<TargetService> findById(long id) {
        try (Connection connection = JdbcSupport.openConnection();
            PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load target service " + id, ex);
        }
    }

    public List<TargetService> findAll() {
        try (Connection connection = JdbcSupport.openConnection();
            PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL);
            ResultSet rs = statement.executeQuery()) {
            List<TargetService> services = new ArrayList<>();
            while (rs.next()) {
                services.add(mapRow(rs));
            }
            return services;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to list target services", ex);
        }
    }

    public boolean delete(long id) {
        try (Connection connection = JdbcSupport.openConnection();
            PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to delete target service " + id, ex);
        }
    }

    public boolean isEmpty() {
        try (Connection connection = JdbcSupport.openConnection();
            PreparedStatement statement = connection.prepareStatement(COUNT_SQL);
            ResultSet rs = statement.executeQuery()) {
            rs.next();
            return rs.getLong(1) == 0;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to count target services", ex);
        }
    }

    private static TargetService mapRow(ResultSet rs) throws SQLException {
        TargetService service = new TargetService(
            rs.getLong("id"),
            rs.getLong("project_id"),
            rs.getString("name"),
            rs.getString("base_url"),
            rs.getString("health_endpoint"),
            rs.getString("environment"),
            rs.getInt("timeout_threshold_ms")
        );
        service.setStatus(ServiceStatus.valueOf(rs.getString("status")));
        return service;
    }
}
