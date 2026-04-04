package com.microchaos.swing.ui;

import com.microchaos.swing.api.ApiClient;
import com.microchaos.swing.model.DashboardOverview;
import com.microchaos.swing.model.Service;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class DashboardPanel extends JPanel {
    private final ApiClient apiClient;
    private JLabel totalServicesLabel;
    private JLabel totalExperimentsLabel;
    private JLabel activeRunsLabel;
    private JLabel avgResilienceLabel;
    private JLabel healthyCountLabel;
    private JLabel degradedCountLabel;
    private JLabel downCountLabel;
    private TopologyGraphPanel topologyPanel;

    public DashboardPanel(ApiClient apiClient) {
        this.apiClient = apiClient;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setBackground(new Color(20, 25, 45));

        // Title
        JLabel titleLabel = new JLabel("System Overview");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(new Color(100, 180, 220));
        add(titleLabel, BorderLayout.NORTH);

        // Main content panel (left metrics + right topology)
        JPanel contentPanel = new JPanel(new BorderLayout(15, 15));
        contentPanel.setBackground(new Color(20, 25, 45));

        // Left panel - Metric cards
        JPanel metricsPanel = createMetricsPanel();
        contentPanel.add(metricsPanel, BorderLayout.WEST);

        // Right panel - Topology graph
        topologyPanel = new TopologyGraphPanel();
        contentPanel.add(topologyPanel, BorderLayout.CENTER);

        add(contentPanel, BorderLayout.CENTER);

        // Refresh button
        JButton refreshButton = new JButton("Refresh");
        refreshButton.setPreferredSize(new Dimension(100, 40));
        refreshButton.addActionListener(e -> refresh());
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(new Color(20, 25, 45));
        buttonPanel.add(refreshButton);
        add(buttonPanel, BorderLayout.SOUTH);

        refresh();

        // Auto-refresh every 5 seconds
        Timer timer = new Timer(5000, e -> refresh());
        timer.start();
    }

    private JPanel createMetricsPanel() {
        JPanel panel = new JPanel(new GridLayout(7, 1, 10, 10));
        panel.setBackground(new Color(20, 25, 45));
        panel.setPreferredSize(new Dimension(250, 400));

        // Add metric cards
        panel.add(createMetricCard("Total Services", totalServicesLabel = new JLabel("--")));
        panel.add(createMetricCard("Experiments", totalExperimentsLabel = new JLabel("--")));
        panel.add(createMetricCard("Active Runs", activeRunsLabel = new JLabel("--")));
        panel.add(createMetricCard("Resilience", avgResilienceLabel = new JLabel("--")));
        panel.add(createMetricCard("Healthy", healthyCountLabel = new JLabel("--")));
        panel.add(createMetricCard("Degraded", degradedCountLabel = new JLabel("--")));
        panel.add(createMetricCard("Down", downCountLabel = new JLabel("--")));

        return panel;
    }

    private JPanel createMetricCard(String label, JLabel valueLabel) {
        JPanel card = new JPanel(new BorderLayout(5, 5));
        card.setBackground(new Color(35, 45, 75));
        card.setBorder(BorderFactory.createLineBorder(new Color(60, 120, 180), 2));
        card.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel(label);
        titleLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        titleLabel.setForeground(new Color(150, 180, 220));

        valueLabel.setFont(new Font("Arial", Font.BOLD, 24));
        valueLabel.setForeground(new Color(100, 200, 255));
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private void refresh() {
        new Thread(() -> {
            try {
                DashboardOverview overview = apiClient.get("/dashboard/overview", DashboardOverview.class);
                List<Service> services = apiClient.getList("/services", Service.class);
                
                SwingUtilities.invokeLater(() -> {
                    totalServicesLabel.setText(String.valueOf(overview.totalServices));
                    totalExperimentsLabel.setText(String.valueOf(overview.totalExperiments));
                    activeRunsLabel.setText(String.valueOf(overview.activeRuns));
                    avgResilienceLabel.setText(String.format("%.1f", overview.averageResilienceScore));
                    if (overview.monitoring != null) {
                        healthyCountLabel.setText(String.valueOf(overview.monitoring.healthyCount));
                        degradedCountLabel.setText(String.valueOf(overview.monitoring.degradedCount));
                        downCountLabel.setText(String.valueOf(overview.monitoring.downCount));
                    }
                    
                    // Update topology
                    topologyPanel.updateServices(services);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // Inner class for topology graph visualization
    private static class TopologyGraphPanel extends JPanel {
        private List<Service> services;

        public TopologyGraphPanel() {
            setBackground(new Color(20, 25, 45));
            setBorder(BorderFactory.createLineBorder(new Color(60, 70, 100), 1));
        }

        public void updateServices(List<Service> services) {
            this.services = services;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw title
            g2d.setColor(new Color(100, 180, 220));
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.drawString("Topology Graph", 10, 25);

            if (services == null || services.isEmpty()) {
                g2d.setColor(new Color(150, 150, 150));
                g2d.drawString("Loading services...", 10, getHeight() / 2);
                return;
            }

            int radius = 25;
            int centerX = getWidth() / 2;
            int centerY = getHeight() / 2;
            
            // Draw services as circles
            for (int i = 0; i < services.size(); i++) {
                Service service = services.get(i);
                double angle = (2 * Math.PI * i) / services.size() - Math.PI / 2;
                int x = (int) (centerX + 70 * Math.cos(angle));
                int y = (int) (centerY + 70 * Math.sin(angle));

                // Draw circle
                g2d.setColor(new Color(50, 150, 120));
                g2d.fillOval(x - radius, y - radius, radius * 2, radius * 2);
                
                g2d.setColor(new Color(100, 200, 150));
                g2d.setStroke(new BasicStroke(2));
                g2d.drawOval(x - radius, y - radius, radius * 2, radius * 2);

                // Draw service name
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.PLAIN, 10));
                String name = service.name.length() > 12 ? service.name.substring(0, 12) : service.name;
                g2d.drawString(name, x - 20, y + 5);
            }

            // Draw connections from center
            g2d.setColor(new Color(100, 150, 200));
            g2d.setStroke(new BasicStroke(1.5f));
            for (int i = 0; i < services.size(); i++) {
                double angle = (2 * Math.PI * i) / services.size() - Math.PI / 2;
                int x = (int) (centerX + 70 * Math.cos(angle));
                int y = (int) (centerY + 70 * Math.sin(angle));
                g2d.drawLine(centerX, centerY, x, y);
            }
        }
    }
}
