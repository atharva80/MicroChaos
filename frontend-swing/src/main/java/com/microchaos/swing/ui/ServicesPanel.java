package com.microchaos.swing.ui;

import com.microchaos.swing.api.ApiClient;
import com.microchaos.swing.model.Service;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ServicesPanel extends JPanel {
    private final ApiClient apiClient;
    private JTable servicesTable;
    private DefaultTableModel tableModel;
    private JTextField nameField;
    private JTextField baseUrlField;
    private JTextField envField;

    public ServicesPanel(ApiClient apiClient) {
        this.apiClient = apiClient;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setBackground(new Color(20, 25, 45));

        // Title
        JLabel titleLabel = new JLabel("Service Registry");
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

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refresh());
        panel.add(refreshButton);

        return panel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(20, 25, 45));

        tableModel = new DefaultTableModel(
            new String[]{"ID", "Name", "Base URL", "Environment", "Status"},
            0
        );
        servicesTable = new JTable(tableModel);
        servicesTable.setBackground(new Color(30, 35, 60));
        servicesTable.setForeground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(servicesTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void addService() {
        String name = nameField.getText().trim();
        String baseUrl = baseUrlField.getText().trim();
        String env = envField.getText().trim();

        if (name.isEmpty() || baseUrl.isEmpty() || env.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required!");
            return;
        }

        new Thread(() -> {
            try {
                String endpoint = "/services?name=" + name + "&baseUrl=" + baseUrl + "&environment=" + env + "&projectId=1";
                apiClient.post(endpoint, String.class);
                SwingUtilities.invokeLater(() -> {
                    nameField.setText("");
                    baseUrlField.setText("");
                    envField.setText("");
                    refresh();
                });
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error adding service: " + e.getMessage());
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
                            service.status
                        });
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
