package com.microchaos.swing.ui;

import com.microchaos.swing.api.ApiClient;
import com.microchaos.swing.model.Service;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

public class ServicesPanel extends JPanel {
    private final ApiClient apiClient;
    private final DefaultTableModel tableModel;
    private final JTable servicesTable;
    private JTextField nameField;
    private JTextField baseUrlField;
    private JTextField envField;

    public ServicesPanel(ApiClient apiClient) {
        this.apiClient = apiClient;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setBackground(new Color(20, 25, 45));

        JPanel headerPanel = new JPanel(new BorderLayout(0, 10));
        headerPanel.setOpaque(false);
        headerPanel.add(createTitleLabel(), BorderLayout.NORTH);
        headerPanel.add(createFormPanel(), BorderLayout.CENTER);

        tableModel = new DefaultTableModel(
            new String[]{"ID", "Name", "Base URL", "Environment", "Health", "Timeout", "Status"},
            0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        servicesTable = createTable();

        add(headerPanel, BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);

        refresh();
    }

    private JLabel createTitleLabel() {
        JLabel titleLabel = new JLabel("Service Registry");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        return titleLabel;
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panel.setBackground(new Color(30, 40, 70));
        panel.setBorder(BorderFactory.createLineBorder(new Color(60, 70, 100)));

        panel.add(new JLabel("Service Name:"));
        nameField = new JTextField(15);
        panel.add(nameField);

        panel.add(new JLabel("Base URL:"));
        baseUrlField = new JTextField(20);
        panel.add(baseUrlField);

        panel.add(new JLabel("Environment:"));
        envField = new JTextField(8);
        panel.add(envField);

        JButton addButton = new JButton("Add Service");
        addButton.addActionListener(e -> addService());
        panel.add(addButton);

        JButton deleteButton = new JButton("Delete Selected");
        deleteButton.addActionListener(e -> deleteSelectedService());
        panel.add(deleteButton);

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
        panel.add(new JScrollPane(servicesTable), BorderLayout.CENTER);
        return panel;
    }

    private void addService() {
        String name = nameField.getText().trim();
        String baseUrl = baseUrlField.getText().trim();
        String env = envField.getText().trim();

        if (name.isEmpty() || baseUrl.isEmpty() || env.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required.");
            return;
        }

        new Thread(() -> {
            try {
                String endpoint = "/services?projectId=1"
                    + "&name=" + encode(name)
                    + "&baseUrl=" + encode(baseUrl)
                    + "&environment=" + encode(env);
                apiClient.post(endpoint, Service.class);
                SwingUtilities.invokeLater(() -> {
                    nameField.setText("");
                    baseUrlField.setText("");
                    envField.setText("");
                    refresh();
                });
            } catch (Exception e) {
                showError("Error adding service: " + e.getMessage());
            }
        }).start();
    }

    private void deleteSelectedService() {
        int selectedRow = servicesTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Select a service to delete.");
            return;
        }

        long serviceId = ((Number) tableModel.getValueAt(selectedRow, 0)).longValue();
        String serviceName = String.valueOf(tableModel.getValueAt(selectedRow, 1));
        int choice = JOptionPane.showConfirmDialog(
            this,
            "Delete service \"" + serviceName + "\"?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION
        );
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        new Thread(() -> {
            try {
                apiClient.delete("/services/" + serviceId);
                SwingUtilities.invokeLater(this::refresh);
            } catch (Exception e) {
                showError("Error deleting service: " + e.getMessage());
            }
        }).start();
    }

    private void refresh() {
        new Thread(() -> {
            try {
                List<Service> services = apiClient.getList("/services", Service.class);
                SwingUtilities.invokeLater(() -> {
                    tableModel.setRowCount(0);
                    for (Service service : services) {
                        tableModel.addRow(new Object[]{
                            service.id,
                            service.name,
                            service.baseUrl,
                            service.environment,
                            service.healthEndpoint,
                            service.timeoutThresholdMs,
                            service.status
                        });
                    }
                });
            } catch (Exception e) {
                showError("Error loading services: " + e.getMessage());
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
