package com.microchaos.swing;

import com.microchaos.swing.api.ApiClient;
import com.microchaos.swing.ui.*;

import javax.swing.*;
import java.awt.*;

public class MicroChaosSwingApp {
    private JFrame frame;
    private ApiClient apiClient;

    public MicroChaosSwingApp() {
        String apiBase = System.getProperty("api.base", "http://localhost:8080/api");
        this.apiClient = new ApiClient(apiBase);
        initializeUI();
    }

    private void initializeUI() {
        // Set dark theme
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        frame = new JFrame("MicroChaos - Chaos Engineering Platform");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1400, 900);
        frame.setLocationRelativeTo(null);
        frame.setBackground(new Color(20, 25, 45));

        // Create tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(new Color(30, 35, 60));
        tabbedPane.setForeground(new Color(100, 180, 220));
        
        // Set tab font to be more visible
        tabbedPane.setFont(new Font("Arial", Font.BOLD, 12));
        
        // Add tabs
        tabbedPane.addTab("Dashboard", new DashboardPanel(apiClient));
        tabbedPane.addTab("Services", new ServicesPanel(apiClient));
        tabbedPane.addTab("Experiments", new ExperimentsPanel(apiClient));
        tabbedPane.addTab("Monitoring", new MonitoringPanel(apiClient));
        tabbedPane.addTab("Runs", new RunsPanel(apiClient));

        // Set UI properties for better visibility
        UIManager.put("TabbedPane.foreground", new Color(100, 180, 220));
        UIManager.put("TabbedPane.selectedForeground", new Color(255, 255, 140));
        
        frame.add(tabbedPane);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MicroChaosSwingApp());
    }
}
