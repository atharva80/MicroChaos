package com.microchaos.backend.core;

import com.microchaos.backend.model.ExperimentRun;
import com.microchaos.backend.model.RunStatus;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ExperimentRunRepository {
    private static final String INSERT_SQL =
        """
        INSERT INTO experiment_runs (experiment_id, status, mttr_seconds, resilience_score, summary)
        VALUES (?, ?, ?, ?, ?)
        RETURNING id, started_at
        """;

    private static final String UPDATE_SQL =
        """
        UPDATE experiment_runs
        SET ended_at = ?, status = ?, mttr_seconds = ?, resilience_score = ?, summary = ?
        WHERE id = ?
        """;

    private static final String FIND_ALL_SQL =
        """
        SELECT id, experiment_id, started_at, ended_at, status, mttr_seconds, resilience_score, summary
        FROM experiment_runs
        ORDER BY id
        """;

    public ExperimentRun create(long experimentId) {
        try (Connection connection = JdbcSupport.openConnection();
            PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            statement.setLong(1, experimentId);
            statement.setString(2, RunStatus.RUNNING.name());
            statement.setLong(3, 0L);
            statement.setDouble(4, 0.0);
            statement.setString(5, "");
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException("Failed to create experiment run");
                }
                return new ExperimentRun(
                    rs.getLong("id"),
                    experimentId,
                    rs.getTimestamp("started_at").toInstant(),
                    null,
                    RunStatus.RUNNING,
                    "",
                    0L,
                    0.0
                );
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to create experiment run", ex);
        }
    }

    public void update(ExperimentRun run) {
        try (Connection connection = JdbcSupport.openConnection();
            PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            statement.setTimestamp(1, run.getEndedAt() == null ? null : Timestamp.from(run.getEndedAt()));
            statement.setString(2, run.getStatus().name());
            statement.setLong(3, run.getMttrSeconds());
            statement.setDouble(4, run.getResilienceScore());
            statement.setString(5, run.getSummary());
            statement.setLong(6, run.getId());
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("Run not found: " + run.getId());
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to update experiment run " + run.getId(), ex);
        }
    }

    public Optional<ExperimentRun> findById(long id) {
        try (Connection connection = JdbcSupport.openConnection();
            PreparedStatement statement = connection.prepareStatement(
                """
                SELECT id, experiment_id, started_at, ended_at, status, mttr_seconds, resilience_score, summary
                FROM experiment_runs
                WHERE id = ?
                """
            )) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load experiment run " + id, ex);
        }
    }

    public List<ExperimentRun> findAll() {
        try (Connection connection = JdbcSupport.openConnection();
            PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL);
            ResultSet rs = statement.executeQuery()) {
            List<ExperimentRun> runs = new ArrayList<>();
            while (rs.next()) {
                runs.add(mapRow(rs));
            }
            return runs;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to list experiment runs", ex);
        }
    }

    public Optional<ExperimentRun> findLatestByExperimentId(long experimentId) {
        try (Connection connection = JdbcSupport.openConnection();
            PreparedStatement statement = connection.prepareStatement(
                """
                SELECT id, experiment_id, started_at, ended_at, status, mttr_seconds, resilience_score, summary
                FROM experiment_runs
                WHERE experiment_id = ?
                ORDER BY id DESC
                LIMIT 1
                """
            )) {
            statement.setLong(1, experimentId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load latest run for experiment " + experimentId, ex);
        }
    }

    private static ExperimentRun mapRow(ResultSet rs) throws SQLException {
        Timestamp endedAt = rs.getTimestamp("ended_at");
        return new ExperimentRun(
            rs.getLong("id"),
            rs.getLong("experiment_id"),
            rs.getTimestamp("started_at").toInstant(),
            endedAt == null ? null : endedAt.toInstant(),
            RunStatus.valueOf(rs.getString("status")),
            rs.getString("summary"),
            rs.getLong("mttr_seconds"),
            rs.getDouble("resilience_score")
        );
    }
}
