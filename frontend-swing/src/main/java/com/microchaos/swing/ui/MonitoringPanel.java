package com.microchaos.swing.ui;

import com.microchaos.swing.api.ApiClient;
import com.microchaos.swing.model.MonitoringData;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class MonitoringPanel extends JPanel {
    private final ApiClient apiClient;
    private JTable monitoringTable;
    private DefaultTableModel tableModel;

    public MonitoringPanel(ApiClient apiClient) {
        this.apiClient = apiClient;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setBackground(new Color(20, 25, 45));

        JLabel titleLabel = new JLabel("Live Monitoring");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);

        // Create table
        tableModel = new DefaultTableModel(
            new String[]{"Service Name", "Status", "Response Time (ms)", "Error Rate", "Throughput", "Availability %", "Actions"},
            0
        );
        monitoringTable = new JTable(tableModel);
        monitoringTable.setBackground(new Color(30, 35, 60));
        monitoringTable.setForeground(Color.WHITE);
        monitoringTable.setRowHeight(40);

        JScrollPane scrollPane = new JScrollPane(monitoringTable);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.setBackground(new Color(20, 25, 45));
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refresh());
        buttonPanel.add(refreshButton);

        // Add components
        add(titleLabel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        refresh();

        // Auto-refresh every 3 seconds
        Timer timer = new Timer(3000, e -> refresh());
        timer.start();
    }

    private void refresh() {
        new Thread(() -> {
            try {
                List<MonitoringData> monitoring = apiClient.getList("/monitoring/services", MonitoringData.class);
                SwingUtilities.invokeLater(() -> {
                    tableModel.setRowCount(0);
                    for (MonitoringData data : monitoring) {
                        tableModel.addRow(new Object[]{
                            data.serviceName,
                            data.status,
                            String.format("%.2f", data.responseTimeMs),
                            String.format("%.2f%%", data.errorRate),
                            String.format("%.2f", data.throughput),
                            String.format("%.2f%%", data.availabilityPercent),
                            "Down | Latency | Recover"
                        });
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
