package com.microchaos.backend.core;

import com.microchaos.backend.model.MetricSnapshot;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class MetricSnapshotRepository {
    private static final String INSERT_SQL =
        """
        INSERT INTO metric_snapshots (
            run_id, ts, response_time_ms, error_rate, throughput, p95_latency_ms, availability_percent
        ) VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

    private static final String FIND_BY_RUN_SQL =
        """
        SELECT id, run_id, ts, response_time_ms, error_rate, throughput, p95_latency_ms, availability_percent
        FROM metric_snapshots
        WHERE run_id = ?
        ORDER BY id
        """;

    public void saveAll(List<MetricSnapshot> metrics) {
        if (metrics.isEmpty()) {
            return;
        }
        try (Connection connection = JdbcSupport.openConnection();
            PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            for (MetricSnapshot metric : metrics) {
                statement.setLong(1, metric.getRunId());
                statement.setTimestamp(2, Timestamp.from(metric.getTimestamp()));
                statement.setDouble(3, metric.getResponseTimeMs());
                statement.setDouble(4, metric.getErrorRate());
                statement.setDouble(5, metric.getThroughput());
                statement.setDouble(6, metric.getP95LatencyMs());
                statement.setDouble(7, metric.getAvailabilityPercent());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to save metric snapshots", ex);
        }
    }

    public List<MetricSnapshot> findByRunId(long runId) {
        try (Connection connection = JdbcSupport.openConnection();
            PreparedStatement statement = connection.prepareStatement(FIND_BY_RUN_SQL)) {
            statement.setLong(1, runId);
            try (ResultSet rs = statement.executeQuery()) {
                List<MetricSnapshot> metrics = new ArrayList<>();
                while (rs.next()) {
                    metrics.add(mapRow(rs));
                }
                return metrics;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to list metrics for run " + runId, ex);
        }
    }

    private static MetricSnapshot mapRow(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("ts");
        return new MetricSnapshot(
            rs.getLong("id"),
            rs.getLong("run_id"),
            ts == null ? Instant.now() : ts.toInstant(),
            rs.getDouble("response_time_ms"),
            rs.getDouble("error_rate"),
            rs.getDouble("throughput"),
            rs.getDouble("p95_latency_ms"),
            rs.getDouble("availability_percent")
        );
    }
}
