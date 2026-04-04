package com.microchaos.swing.ui;

import com.microchaos.swing.api.ApiClient;
import com.microchaos.swing.model.Experiment;
import com.microchaos.swing.model.Service;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

public class ExperimentsPanel extends JPanel {
    private final ApiClient apiClient;
    private final Map<Long, Service> servicesById = new LinkedHashMap<>();
    private final DefaultTableModel tableModel;
    private final JTable experimentsTable;
    private JComboBox<Service> targetServiceCombo;
    private JComboBox<String> faultTypeCombo;
    private JComboBox<String> stressTypeCombo;
    private JTextField nameField;
    private JSpinner intensitySpinner;

    public ExperimentsPanel(ApiClient apiClient) {
        this.apiClient = apiClient;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setBackground(new Color(20, 25, 45));

        JPanel headerPanel = new JPanel(new BorderLayout(0, 10));
        headerPanel.setOpaque(false);
        headerPanel.add(createTitleLabel(), BorderLayout.NORTH);
        headerPanel.add(createFormPanel(), BorderLayout.CENTER);

        tableModel = new DefaultTableModel(
            new String[]{"ID", "Name", "Target Service", "Fault Type", "Stress Type", "Intensity", "Duration (s)", "Status"},
            0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        experimentsTable = createTable();

        add(headerPanel, BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);

        refresh();
    }

    private JLabel createTitleLabel() {
        JLabel titleLabel = new JLabel("Experiment Studio");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        return titleLabel;
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
        faultTypeCombo = new JComboBox<>(new String[]{"LATENCY", "TIMEOUT", "ERROR", "CRASH", "DEPENDENCY_UNAVAILABLE"});
        panel.add(faultTypeCombo);

        panel.add(new JLabel("Stress Type:"));
        stressTypeCombo = new JComboBox<>(new String[]{"BURST", "STEADY", "RAMP"});
        panel.add(stressTypeCombo);

        panel.add(new JLabel("Intensity:"));
        intensitySpinner = new JSpinner(new SpinnerNumberModel(50, 0, 100, 10));
        panel.add(intensitySpinner);

        JButton createButton = new JButton("Create");
        createButton.addActionListener(e -> createExperiment());
        panel.add(createButton);

        JButton runButton = new JButton("Run Selected");
        runButton.addActionListener(e -> runSelectedExperiment());
        panel.add(runButton);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refresh());
        panel.add(refreshButton);

        return panel;
    }

    private JTable createTable() {
        JTable table = new JTable(tableModel);
        table.setBackground(new Color(30, 35, 60));
        table.setForeground(Color.WHITE);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(28);
        return table;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(20, 25, 45));
        panel.add(new JScrollPane(experimentsTable), BorderLayout.CENTER);
        return panel;
    }

    private void createExperiment() {
        String name = nameField.getText().trim();
        Service targetService = (Service) targetServiceCombo.getSelectedItem();
        String faultType = String.valueOf(faultTypeCombo.getSelectedItem());
        String stressType = String.valueOf(stressTypeCombo.getSelectedItem());
        int intensity = (Integer) intensitySpinner.getValue();

        if (name.isEmpty() || targetService == null) {
            JOptionPane.showMessageDialog(this, "Experiment name and target service are required.");
            return;
        }

        new Thread(() -> {
            try {
                String endpoint = "/experiments?projectId=1"
                    + "&name=" + encode(name)
                    + "&targetServiceId=" + targetService.id
                    + "&faultType=" + encode(faultType)
                    + "&stressType=" + encode(stressType)
                    + "&intensity=" + intensity
                    + "&durationSeconds=60"
                    + "&blastRadiusLimit=3"
                    + "&createdBy=1";
                apiClient.post(endpoint, Experiment.class);
                SwingUtilities.invokeLater(() -> {
                    nameField.setText("");
                    refresh();
                });
            } catch (Exception e) {
                showError("Error creating experiment: " + e.getMessage());
            }
        }).start();
    }

    private void runSelectedExperiment() {
        int selectedRow = experimentsTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Select an experiment to run.");
            return;
        }

        long experimentId = ((Number) tableModel.getValueAt(selectedRow, 0)).longValue();
        String experimentName = String.valueOf(tableModel.getValueAt(selectedRow, 1));

        new Thread(() -> {
            try {
                String response = apiClient.post("/experiments/" + experimentId + "/run", String.class);
                SwingUtilities.invokeLater(() -> {
                    refresh();
                    JOptionPane.showMessageDialog(
                        this,
                        "Experiment \"" + experimentName + "\" executed.\n\n" + response,
                        "Run Created",
                        JOptionPane.INFORMATION_MESSAGE
                    );
                });
            } catch (Exception e) {
                showError("Error running experiment: " + e.getMessage());
            }
        }).start();
    }

    private void refresh() {
        new Thread(() -> {
            try {
                List<Service> services = apiClient.getList("/services", Service.class);
                List<Experiment> experiments = apiClient.getList("/experiments", Experiment.class);

                servicesById.clear();
                for (Service service : services) {
                    servicesById.put(service.id, service);
                }

                SwingUtilities.invokeLater(() -> {
                    Service selectedService = (Service) targetServiceCombo.getSelectedItem();
                    targetServiceCombo.removeAllItems();
                    for (Service service : services) {
                        targetServiceCombo.addItem(service);
                    }
                    if (selectedService != null) {
                        targetServiceCombo.setSelectedItem(selectedService);
                    }

                    tableModel.setRowCount(0);
                    for (Experiment experiment : experiments) {
                        String serviceName = servicesById.containsKey(experiment.targetServiceId)
                            ? servicesById.get(experiment.targetServiceId).name
                            : "Unknown";
                        tableModel.addRow(new Object[]{
                            experiment.id,
                            experiment.name,
                            serviceName,
                            experiment.faultType,
                            experiment.stressType,
                            experiment.intensity,
                            experiment.durationSeconds,
                            experiment.status
                        });
                    }
                });
            } catch (Exception e) {
                showError("Error loading experiments: " + e.getMessage());
            }
        }).start();
    }

    private void showError(String message) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, message));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
