package com.microchaos.swing.ui;

import com.microchaos.swing.api.ApiClient;
import com.microchaos.swing.model.ExperimentRun;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;

public class RunsPanel extends JPanel {
    private final ApiClient apiClient;
    private final DefaultTableModel tableModel;
    private final JTable runsTable;
    private final JTextArea detailsArea;

    public RunsPanel(ApiClient apiClient) {
        this.apiClient = apiClient;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setBackground(new Color(20, 25, 45));

        JLabel titleLabel = new JLabel("Experiment Runs & Analytics");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);

        tableModel = new DefaultTableModel(
            new String[]{"ID", "Experiment ID", "Status", "Resilience Score", "MTTR (s)", "Started", "Ended", "Summary"},
            0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        runsTable = new JTable(tableModel);
        runsTable.setBackground(new Color(30, 35, 60));
        runsTable.setForeground(Color.WHITE);
        runsTable.setRowHeight(28);
        runsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        runsTable.getSelectionModel().addListSelectionListener(e -> updateDetailsForSelection());

        detailsArea = new JTextArea(8, 20);
        detailsArea.setEditable(false);
        detailsArea.setLineWrap(true);
        detailsArea.setWrapStyleWord(true);
        detailsArea.setBackground(new Color(18, 22, 38));
        detailsArea.setForeground(Color.WHITE);
        detailsArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        detailsArea.setText("Run details will appear here.");

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.setBackground(new Color(20, 25, 45));

        JButton refreshButton = new JButton("Refresh Runs");
        refreshButton.addActionListener(e -> refresh());
        buttonPanel.add(refreshButton);

        JButton metricsButton = new JButton("Metrics");
        metricsButton.addActionListener(e -> loadRunResource("metrics"));
        buttonPanel.add(metricsButton);

        JButton scorecardButton = new JButton("Scorecard");
        scorecardButton.addActionListener(e -> loadRunResource("scorecard"));
        buttonPanel.add(scorecardButton);

        JButton rawButton = new JButton("Raw");
        rawButton.addActionListener(e -> loadRawRun());
        buttonPanel.add(rawButton);

        JPanel bottomPanel = new JPanel(new BorderLayout(0, 10));
        bottomPanel.setOpaque(false);
        bottomPanel.add(buttonPanel, BorderLayout.NORTH);
        bottomPanel.add(new JScrollPane(detailsArea), BorderLayout.CENTER);

        add(titleLabel, BorderLayout.NORTH);
        add(new JScrollPane(runsTable), BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        refresh();
        new Timer(5000, e -> refresh()).start();
    }

    private void refresh() {
        new Thread(() -> {
            try {
                List<ExperimentRun> runs = apiClient.getList("/runs", ExperimentRun.class);
                SwingUtilities.invokeLater(() -> {
                    Object selectedRunId = getSelectedRunId();
                    tableModel.setRowCount(0);
                    for (ExperimentRun run : runs) {
                        tableModel.addRow(new Object[]{
                            run.id,
                            run.experimentId,
                            run.status,
                            String.format("%.2f", run.resilienceScore),
                            run.mttrSeconds,
                            formatDateTime(run.startedAt),
                            formatDateTime(run.endedAt),
                            run.summary
                        });
                    }
                    restoreSelection(selectedRunId);
                    updateDetailsForSelection();
                });
            } catch (Exception e) {
                showError("Error loading runs: " + e.getMessage());
            }
        }).start();
    }

    private void loadRunResource(String resource) {
        Long runId = getSelectedRunId();
        if (runId == null) {
            JOptionPane.showMessageDialog(this, "Select a run first.");
            return;
        }

        new Thread(() -> {
            try {
                String payload = apiClient.get("/runs/" + runId + "/" + resource, String.class);
                SwingUtilities.invokeLater(() -> detailsArea.setText(payload));
            } catch (Exception e) {
                showError("Error loading run " + resource + ": " + e.getMessage());
            }
        }).start();
    }

    private void loadRawRun() {
        Long runId = getSelectedRunId();
        if (runId == null) {
            JOptionPane.showMessageDialog(this, "Select a run first.");
            return;
        }

        new Thread(() -> {
            try {
                String payload = apiClient.get("/runs/" + runId, String.class);
                SwingUtilities.invokeLater(() -> detailsArea.setText(payload));
            } catch (Exception e) {
                showError("Error loading run details: " + e.getMessage());
            }
        }).start();
    }

    private void updateDetailsForSelection() {
        int selectedRow = runsTable.getSelectedRow();
        if (selectedRow < 0) {
            detailsArea.setText("Run details will appear here.");
            return;
        }

        detailsArea.setText(
            "Run #" + tableModel.getValueAt(selectedRow, 0) + "\n"
                + "Experiment ID: " + tableModel.getValueAt(selectedRow, 1) + "\n"
                + "Status: " + tableModel.getValueAt(selectedRow, 2) + "\n"
                + "Resilience score: " + tableModel.getValueAt(selectedRow, 3) + "\n"
                + "MTTR: " + tableModel.getValueAt(selectedRow, 4) + " s\n"
                + "Started: " + tableModel.getValueAt(selectedRow, 5) + "\n"
                + "Ended: " + tableModel.getValueAt(selectedRow, 6) + "\n"
                + "Summary: " + tableModel.getValueAt(selectedRow, 7)
        );
    }

    private Long getSelectedRunId() {
        int selectedRow = runsTable.getSelectedRow();
        if (selectedRow < 0) {
            return null;
        }
        return ((Number) tableModel.getValueAt(selectedRow, 0)).longValue();
    }

    private void restoreSelection(Object runId) {
        if (!(runId instanceof Number)) {
            return;
        }
        Number selectedId = (Number) runId;
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            Number rowId = (Number) tableModel.getValueAt(row, 0);
            if (rowId.longValue() == selectedId.longValue()) {
                runsTable.setRowSelectionInterval(row, row);
                break;
            }
        }
    }

    private void showError(String message) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, message));
    }

    private String formatDateTime(String dateTime) {
        if (dateTime == null || dateTime.isBlank()) {
            return "-";
        }
        try {
            if (dateTime.contains("T")) {
                return dateTime.replace('T', ' ').replace("Z", "");
            }
        } catch (Exception ignored) {
        }
        return dateTime;
    }
}
