package com.microchaos.backend.core;

import com.microchaos.backend.model.Experiment;
import com.microchaos.backend.model.ExperimentStatus;
import com.microchaos.backend.model.FaultType;
import com.microchaos.backend.model.RemediationMode;
import com.microchaos.backend.model.StressType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ExperimentRepository {
    private static final String INSERT_SQL =
        """
        INSERT INTO experiments (
            project_id, name, description, target_service_id, fault_type, stress_type, duration_seconds,
            intensity, remediation_mode, blast_radius_limit, status, created_by
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        RETURNING id, created_at
        """;

    private static final String FIND_ALL_SQL =
        """
        SELECT id, project_id, name, description, target_service_id, fault_type, stress_type, duration_seconds,
               intensity, remediation_mode, blast_radius_limit, status, created_by, created_at
        FROM experiments
        ORDER BY id
        """;

    private static final String UPDATE_STATUS_SQL = "UPDATE experiments SET status = ? WHERE id = ?";

    public Experiment create(Experiment experiment) {
        try (Connection connection = JdbcSupport.openConnection();
            PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            statement.setLong(1, experiment.getProjectId());
            statement.setString(2, experiment.getName());
            statement.setString(3, experiment.getDescription());
            statement.setLong(4, experiment.getTargetServiceId());
            statement.setString(5, experiment.getFaultType().name());
            statement.setString(6, experiment.getStressType().name());
            statement.setInt(7, experiment.getDurationSeconds());
            statement.setInt(8, experiment.getIntensity());
            statement.setString(9, experiment.getRemediationMode().name());
            statement.setInt(10, experiment.getBlastRadiusLimit());
            statement.setString(11, experiment.getStatus().name());
            statement.setLong(12, experiment.getCreatedBy());
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException("Failed to create experiment");
                }
                return new Experiment(
                    rs.getLong("id"),
                    experiment.getProjectId(),
                    experiment.getName(),
                    experiment.getDescription(),
                    experiment.getTargetServiceId(),
                    experiment.getFaultType(),
                    experiment.getStressType(),
                    experiment.getDurationSeconds(),
                    experiment.getIntensity(),
                    experiment.getRemediationMode(),
                    experiment.getBlastRadiusLimit(),
                    experiment.getStatus(),
                    experiment.getCreatedBy(),
                    rs.getTimestamp("created_at").toInstant()
                );
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to create experiment", ex);
        }
    }

    public List<Experiment> findAll() {
        try (Connection connection = JdbcSupport.openConnection();
            PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL);
            ResultSet rs = statement.executeQuery()) {
            List<Experiment> experiments = new ArrayList<>();
            while (rs.next()) {
                experiments.add(mapRow(rs));
            }
            return experiments;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to list experiments", ex);
        }
    }

    public Optional<Experiment> findById(long id) {
        try (Connection connection = JdbcSupport.openConnection();
            PreparedStatement statement = connection.prepareStatement(
                """
                SELECT id, project_id, name, description, target_service_id, fault_type, stress_type, duration_seconds,
                       intensity, remediation_mode, blast_radius_limit, status, created_by, created_at
                FROM experiments
                WHERE id = ?
                """
            )) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load experiment " + id, ex);
        }
    }

    public void updateStatus(long id, ExperimentStatus status) {
        try (Connection connection = JdbcSupport.openConnection();
            PreparedStatement statement = connection.prepareStatement(UPDATE_STATUS_SQL)) {
            statement.setString(1, status.name());
            statement.setLong(2, id);
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("Experiment not found: " + id);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to update experiment " + id, ex);
        }
    }

    private static Experiment mapRow(ResultSet rs) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        return new Experiment(
            rs.getLong("id"),
            rs.getLong("project_id"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getLong("target_service_id"),
            FaultType.from(rs.getString("fault_type")),
            StressType.from(rs.getString("stress_type")),
            rs.getInt("duration_seconds"),
            rs.getInt("intensity"),
            RemediationMode.from(rs.getString("remediation_mode")),
            rs.getInt("blast_radius_limit"),
            ExperimentStatus.valueOf(rs.getString("status")),
            rs.getLong("created_by"),
            createdAt == null ? Instant.now() : createdAt.toInstant()
        );
    }
}
