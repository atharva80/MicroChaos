package com.microchaos.backend.core;

import com.microchaos.backend.model.CommunicationMode;
import com.microchaos.backend.model.Criticality;
import com.microchaos.backend.model.DependencyType;
import com.microchaos.backend.model.ServiceDependency;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ServiceDependencyRepository {
    private static final String INSERT_SQL =
        """
        INSERT INTO service_dependencies (
            source_service_id, target_service_id, dependency_type, protocol, communication_mode, criticality, fallback_available
        ) VALUES (?, ?, ?, ?, ?, ?, ?)
        RETURNING id
        """;

    private static final String FIND_ALL_SQL =
        """
        SELECT id, source_service_id, target_service_id, dependency_type, protocol, communication_mode, criticality, fallback_available
        FROM service_dependencies
        ORDER BY id
        """;

    private static final String COUNT_SQL = "SELECT COUNT(*) FROM service_dependencies";

    public ServiceDependency create(ServiceDependency dependency) {
        try (Connection connection = JdbcSupport.openConnection();
            PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            statement.setLong(1, dependency.getSourceServiceId());
            statement.setLong(2, dependency.getTargetServiceId());
            statement.setString(3, dependency.getDependencyType().name());
            statement.setString(4, dependency.getProtocol());
            statement.setString(5, dependency.getCommunicationMode().name());
            statement.setString(6, dependency.getCriticality().name());
            statement.setBoolean(7, dependency.isFallbackAvailable());

            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException("Failed to insert service dependency");
                }
                return new ServiceDependency(
                    rs.getLong("id"),
                    dependency.getSourceServiceId(),
                    dependency.getTargetServiceId(),
                    dependency.getDependencyType(),
                    dependency.getProtocol(),
                    dependency.getCommunicationMode(),
                    dependency.getCriticality(),
                    dependency.isFallbackAvailable()
                );
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to create service dependency", ex);
        }
    }

    public List<ServiceDependency> findAll() {
        try (Connection connection = JdbcSupport.openConnection();
            PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL);
            ResultSet rs = statement.executeQuery()) {
            List<ServiceDependency> dependencies = new ArrayList<>();
            while (rs.next()) {
                dependencies.add(mapRow(rs));
            }
            return dependencies;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to list service dependencies", ex);
        }
    }

    public boolean isEmpty() {
        try (Connection connection = JdbcSupport.openConnection();
            PreparedStatement statement = connection.prepareStatement(COUNT_SQL);
            ResultSet rs = statement.executeQuery()) {
            rs.next();
            return rs.getLong(1) == 0;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to count service dependencies", ex);
        }
    }

    private static ServiceDependency mapRow(ResultSet rs) throws SQLException {
        return new ServiceDependency(
            rs.getLong("id"),
            rs.getLong("source_service_id"),
            rs.getLong("target_service_id"),
            DependencyType.from(rs.getString("dependency_type")),
            rs.getString("protocol"),
            CommunicationMode.from(rs.getString("communication_mode")),
            Criticality.from(rs.getString("criticality")),
            rs.getBoolean("fallback_available")
        );
    }
}
