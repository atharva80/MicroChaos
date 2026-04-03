package com.microchaos.swing.ui;

import com.microchaos.swing.api.ApiClient;
import com.microchaos.swing.model.Experiment;
import com.microchaos.swing.model.Service;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExperimentsPanel extends JPanel {
    private final ApiClient apiClient;
    private JTable experimentsTable;
    private DefaultTableModel tableModel;
    private Map<Long, String> serviceIdToName;
    private JComboBox<String> targetServiceCombo;
    private JComboBox<String> faultTypeCombo;
    private JTextField nameField;
    private JSpinner intensitySpinner;

    public ExperimentsPanel(ApiClient apiClient) {
        this.apiClient = apiClient;
        this.serviceIdToName = new HashMap<>();
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setBackground(new Color(20, 25, 45));

        // Title
        JLabel titleLabel = new JLabel("Experiment Studio");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);

        // Form Panel
        JPanel formPanel = createFormPanel();

        // Table Panel
        JPanel tablePanel = createTablePanel();

        // Add to main panel
        add(titleLabel, BorderLayout.NORTH);
        add(formPanel, BorderLayout.PAGE_START);
        add(tablePanel, BorderLayout.CENTER);

        refresh();
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panel.setBackground(new Color(30, 40, 70));
        panel.setBorder(BorderFactory.createLineBorder(new Color(60, 70, 100)));

        panel.add(new JLabel("Experiment Name:"));
        nameField = new JTextField(15);
        panel.add(nameField);

        panel.add(new JLabel("Target Service:"));
        targetServiceCombo = new JComboBox<>();
        panel.add(targetServiceCombo);

        panel.add(new JLabel("Fault Type:"));
        faultTypeCombo = new JComboBox<>(new String[]{"LATENCY", "TIMEOUT", "ERROR", "CRASH"});
        panel.add(faultTypeCombo);

        panel.add(new JLabel("Intensity:"));
        intensitySpinner = new JSpinner(new SpinnerNumberModel(50, 0, 100, 10));
        panel.add(intensitySpinner);

        JButton createButton = new JButton("Create");
        createButton.addActionListener(e -> createExperiment());
        panel.add(createButton);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refresh());
        panel.add(refreshButton);

        return panel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(20, 25, 45));

        tableModel = new DefaultTableModel(
            new String[]{"ID", "Name", "Target Service", "Fault Type", "Stress Type", "Intensity", "Duration (s)", "Action"},
            0
        );
        experimentsTable = new JTable(tableModel);
        experimentsTable.setBackground(new Color(30, 35, 60));
        experimentsTable.setForeground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(experimentsTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void createExperiment() {
        String name = nameField.getText().trim();
        String targetService = targetServiceCombo.getSelectedItem() != null ? targetServiceCombo.getSelectedItem().toString() : "";
        String faultType = faultTypeCombo.getSelectedItem().toString();
        int intensity = (Integer) intensitySpinner.getValue();

        if (name.isEmpty() || targetService.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required!");
            return;
        }

        // Find the service ID from name
        long serviceId = 0;
        for (Map.Entry<Long, String> entry : serviceIdToName.entrySet()) {
            if (entry.getValue().equals(targetService)) {
                serviceId = entry.getKey();
                break;
            }
        }

        if (serviceId == 0) {
            JOptionPane.showMessageDialog(this, "Invalid service selected!");
            return;
        }

        final long finalServiceId = serviceId;
        new Thread(() -> {
            try {
                String endpoint = "/experiments?projectId=1&name=" + name + "&targetServiceId=" + finalServiceId + 
                                  "&faultType=" + faultType + "&stressType=BURST&intensity=" + intensity + 
                                  "&durationSeconds=60&blastRadiusLimit=3&createdBy=1";
                apiClient.post(endpoint, String.class);
                SwingUtilities.invokeLater(() -> {
                    nameField.setText("");
                    refresh();
                });
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error creating experiment: " + e.getMessage());
            }
        }).start();
    }

    private void refresh() {
        new Thread(() -> {
            try {
                // Get service mappings
                List<Service> services = apiClient.getList("/services", Service.class);
                serviceIdToName.clear();
                for (Service service : services) {
                    serviceIdToName.put(service.id, service.name);
                }

                SwingUtilities.invokeLater(() -> {
                    targetServiceCombo.removeAllItems();
                    for (String serviceName : serviceIdToName.values()) {
                        targetServiceCombo.addItem(serviceName);
                    }
                });

                List<Experiment> experiments = apiClient.getList("/experiments", Experiment.class);
                SwingUtilities.invokeLater(() -> {
                    tableModel.setRowCount(0);
                    for (Experiment exp : experiments) {
                        String serviceName = serviceIdToName.getOrDefault(exp.targetServiceId, "Unknown");
                        tableModel.addRow(new Object[]{
                            exp.id,
                            exp.name,
                            serviceName,
                            exp.faultType,
                            exp.stressType,
                            exp.intensity,
                            exp.durationSeconds,
                            "Run"
                        });
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
