package com.microchaos.swing.ui;

import com.microchaos.swing.api.ApiClient;
import com.microchaos.swing.model.MonitoringData;
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

public class MonitoringPanel extends JPanel {
    private final ApiClient apiClient;
    private final DefaultTableModel tableModel;
    private final JTable monitoringTable;
    private final JTextArea detailsArea;

    public MonitoringPanel(ApiClient apiClient) {
        this.apiClient = apiClient;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setBackground(new Color(20, 25, 45));

        JLabel titleLabel = new JLabel("Live Monitoring");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);

        tableModel = new DefaultTableModel(
            new String[]{"ID", "Service", "State", "Health", "Health RT", "Latency", "Timeout", "Faults", "Error %", "Requests", "Failure %"},
            0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        monitoringTable = new JTable(tableModel);
        monitoringTable.setBackground(new Color(30, 35, 60));
        monitoringTable.setForeground(Color.WHITE);
        monitoringTable.setRowHeight(28);
        monitoringTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        monitoringTable.getSelectionModel().addListSelectionListener(e -> updateDetailsForSelection());

        detailsArea = new JTextArea(6, 20);
        detailsArea.setEditable(false);
        detailsArea.setLineWrap(true);
        detailsArea.setWrapStyleWord(true);
        detailsArea.setBackground(new Color(18, 22, 38));
        detailsArea.setForeground(Color.WHITE);
        detailsArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        detailsArea.setText("Select a service to view monitoring details.");

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.setBackground(new Color(20, 25, 45));

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refresh());
        buttonPanel.add(refreshButton);

        JButton downButton = new JButton("Down");
        downButton.addActionListener(e -> injectFault("DEPENDENCY_UNAVAILABLE", 100, 300));
        buttonPanel.add(downButton);

        JButton latencyButton = new JButton("Latency");
        latencyButton.addActionListener(e -> injectFault("LATENCY", 60, 180));
        buttonPanel.add(latencyButton);

        JButton recoverButton = new JButton("Recover");
        recoverButton.addActionListener(e -> recoverSelectedService());
        buttonPanel.add(recoverButton);

        JButton historyButton = new JButton("History");
        historyButton.addActionListener(e -> showHistory());
        buttonPanel.add(historyButton);

        JPanel bottomPanel = new JPanel(new BorderLayout(0, 10));
        bottomPanel.setOpaque(false);
        bottomPanel.add(buttonPanel, BorderLayout.NORTH);
        bottomPanel.add(new JScrollPane(detailsArea), BorderLayout.CENTER);

        add(titleLabel, BorderLayout.NORTH);
        add(new JScrollPane(monitoringTable), BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        refresh();
        new Timer(3000, e -> refresh()).start();
    }

    private void refresh() {
        new Thread(() -> {
            try {
                List<MonitoringData> monitoring = apiClient.getList("/monitoring/services", MonitoringData.class);
                SwingUtilities.invokeLater(() -> {
                    Object selectedServiceId = getSelectedServiceId();
                    tableModel.setRowCount(0);
                    for (MonitoringData data : monitoring) {
                        tableModel.addRow(new Object[]{
                            data.serviceId,
                            data.serviceName,
                            data.monitoringState,
                            data.healthStatusCode,
                            data.healthResponseTimeMs,
                            data.latencyMs,
                            data.timeoutMs,
                            data.activeFaultCount,
                            String.format("%.2f", data.injectedErrorRate),
                            data.totalRequests,
                            String.format("%.2f", calculateFailurePercent(data))
                        });
                    }
                    restoreSelection(selectedServiceId);
                    updateDetailsForSelection();
                });
            } catch (Exception e) {
                showError("Error loading monitoring data: " + e.getMessage());
            }
        }).start();
    }

    private void injectFault(String type, int intensity, int durationSeconds) {
        Long serviceId = getSelectedServiceId();
        if (serviceId == null) {
            JOptionPane.showMessageDialog(this, "Select a service first.");
            return;
        }

        new Thread(() -> {
            try {
                apiClient.post(
                    "/services/" + serviceId + "/faults/inject?type=" + type + "&intensity=" + intensity + "&durationSeconds=" + durationSeconds,
                    String.class
                );
                SwingUtilities.invokeLater(this::refresh);
            } catch (Exception e) {
                showError("Error injecting fault: " + e.getMessage());
            }
        }).start();
    }

    private void recoverSelectedService() {
        Long serviceId = getSelectedServiceId();
        if (serviceId == null) {
            JOptionPane.showMessageDialog(this, "Select a service first.");
            return;
        }

        new Thread(() -> {
            try {
                apiClient.post("/services/" + serviceId + "/faults/reset", String.class);
                SwingUtilities.invokeLater(this::refresh);
            } catch (Exception e) {
                showError("Error recovering service: " + e.getMessage());
            }
        }).start();
    }

    private void showHistory() {
        Long serviceId = getSelectedServiceId();
        if (serviceId == null) {
            JOptionPane.showMessageDialog(this, "Select a service first.");
            return;
        }

        new Thread(() -> {
            try {
                List<MonitoringData> history = apiClient.getList("/monitoring/services/" + serviceId + "/history?limit=30", MonitoringData.class);
                StringBuilder builder = new StringBuilder();
                for (MonitoringData item : history) {
                    builder
                        .append(item.timestamp)
                        .append("  state=")
                        .append(item.monitoringState)
                        .append("  health=")
                        .append(item.healthStatusCode)
                        .append("  latency=")
                        .append(item.latencyMs)
                        .append("  faults=")
                        .append(item.activeFaultCount)
                        .append('\n');
                }
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                    this,
                    builder.length() == 0 ? "No monitoring history available." : builder.toString(),
                    "Monitoring History",
                    JOptionPane.INFORMATION_MESSAGE
                ));
            } catch (Exception e) {
                showError("Error loading monitoring history: " + e.getMessage());
            }
        }).start();
    }

    private void updateDetailsForSelection() {
        int selectedRow = monitoringTable.getSelectedRow();
        if (selectedRow < 0) {
            detailsArea.setText("Select a service to view monitoring details.");
            return;
        }

        detailsArea.setText(
            "Service: " + tableModel.getValueAt(selectedRow, 1) + "\n"
                + "State: " + tableModel.getValueAt(selectedRow, 2) + "\n"
                + "Health status: " + tableModel.getValueAt(selectedRow, 3) + "\n"
                + "Health response time: " + tableModel.getValueAt(selectedRow, 4) + " ms\n"
                + "Injected latency: " + tableModel.getValueAt(selectedRow, 5) + " ms\n"
                + "Injected timeout: " + tableModel.getValueAt(selectedRow, 6) + " ms\n"
                + "Active faults: " + tableModel.getValueAt(selectedRow, 7) + "\n"
                + "Injected error rate: " + tableModel.getValueAt(selectedRow, 8) + "%\n"
                + "Total requests: " + tableModel.getValueAt(selectedRow, 9) + "\n"
                + "Failure rate: " + tableModel.getValueAt(selectedRow, 10) + "%"
        );
    }

    private Long getSelectedServiceId() {
        int selectedRow = monitoringTable.getSelectedRow();
        if (selectedRow < 0) {
            return null;
        }
        return ((Number) tableModel.getValueAt(selectedRow, 0)).longValue();
    }

    private void restoreSelection(Object serviceId) {
        if (!(serviceId instanceof Number)) {
            return;
        }
        Number selectedId = (Number) serviceId;
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            Number rowId = (Number) tableModel.getValueAt(row, 0);
            if (rowId.longValue() == selectedId.longValue()) {
                monitoringTable.setRowSelectionInterval(row, row);
                break;
            }
        }
    }

    private double calculateFailurePercent(MonitoringData data) {
        if (data.totalRequests <= 0) {
            return 0.0;
        }
        return (data.failedRequests * 100.0) / data.totalRequests;
    }

    private void showError(String message) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, message));
    }
}
