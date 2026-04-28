package com.microchaos.swing;

import com.microchaos.swing.api.ApiClient;
import com.microchaos.swing.ui.*;
import com.microchaos.swing.ui.theme.ThemeManager;

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
        try {
            ThemeManager.applyGlobalTheme();
        } catch (Exception e) {
            e.printStackTrace();
        }

        frame = new JFrame("MicroChaos - Chaos Engineering Platform");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1400, 900);
        frame.setLocationRelativeTo(null);
        frame.getContentPane().setBackground(ThemeManager.APP_BG);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(ThemeManager.SURFACE);
        tabbedPane.setForeground(ThemeManager.TEXT_MUTED);
        tabbedPane.setFont(ThemeManager.font(14, Font.BOLD));

        tabbedPane.addTab("Dashboard", new DashboardPanel(apiClient));
        tabbedPane.addTab("Services", new ServicesPanel(apiClient));
        tabbedPane.addTab("Experiments", new ExperimentsPanel(apiClient));
        tabbedPane.addTab("Monitoring", new MonitoringPanel(apiClient));
        tabbedPane.addTab("Runs", new RunsPanel(apiClient));

        frame.add(tabbedPane);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MicroChaosSwingApp());
    }
}
