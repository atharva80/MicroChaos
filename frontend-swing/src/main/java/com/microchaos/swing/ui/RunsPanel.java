package com.microchaos.swing.ui;

import com.microchaos.swing.api.ApiClient;
import com.microchaos.swing.model.ExperimentRun;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class RunsPanel extends JPanel {
    private final ApiClient apiClient;
    private JTable runsTable;
    private DefaultTableModel tableModel;

    public RunsPanel(ApiClient apiClient) {
        this.apiClient = apiClient;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setBackground(new Color(20, 25, 45));

        JLabel titleLabel = new JLabel("Experiment Runs & Analytics");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);

        // Create table
        tableModel = new DefaultTableModel(
            new String[]{"ID", "Experiment ID", "Status", "Resilience Score", "MTTR (s)", "Started", "Ended"},
            0
        );
        runsTable = new JTable(tableModel);
        runsTable.setBackground(new Color(30, 35, 60));
        runsTable.setForeground(Color.WHITE);
        runsTable.setRowHeight(30);

        JScrollPane scrollPane = new JScrollPane(runsTable);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.setBackground(new Color(20, 25, 45));
        JButton refreshButton = new JButton("Refresh Runs");
        refreshButton.addActionListener(e -> refresh());
        buttonPanel.add(refreshButton);

        // Add components
        add(titleLabel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        refresh();
    }

    private void refresh() {
        new Thread(() -> {
            try {
                List<ExperimentRun> runs = apiClient.getList("/runs", ExperimentRun.class);
                SwingUtilities.invokeLater(() -> {
                    tableModel.setRowCount(0);
                    for (ExperimentRun run : runs) {
                        tableModel.addRow(new Object[]{
                            run.id,
                            run.experimentId,
                            run.status,
                            String.format("%.2f", run.resilienceScore),
                            run.mttrSeconds,
                            formatDateTime(run.startedAt),
                            run.endedAt != null ? formatDateTime(run.endedAt) : "-"
                        });
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private String formatDateTime(String dateTime) {
        if (dateTime == null) return "-";
        try {
            // Simple format: show just the time portion if it looks like ISO format
            if (dateTime.contains("T")) {
                return dateTime.split("T")[1].substring(0, 8);
            }
        } catch (Exception e) {
            // Ignore
        }
        return dateTime;
    }
}
