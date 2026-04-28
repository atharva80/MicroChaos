package com.microchaos.swing.ui;

import com.microchaos.swing.api.ApiClient;
import com.microchaos.swing.model.DashboardOverview;
import com.microchaos.swing.model.Service;
import com.microchaos.swing.ui.components.CardPanel;
import com.microchaos.swing.ui.theme.ThemeManager;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class DashboardPanel extends JPanel {
    private final ApiClient apiClient;
    private final MetricCard totalServicesCard;
    private final MetricCard totalExperimentsCard;
    private final MetricCard activeRunsCard;
    private final MetricCard avgResilienceCard;
    private final MetricCard healthyCountCard;
    private final MetricCard degradedCountCard;
    private final MetricCard downCountCard;
    private final TopologyGraphPanel topologyPanel;

    public DashboardPanel(ApiClient apiClient) {
        this.apiClient = apiClient;

        setLayout(new BorderLayout(24, 24));
        setBackground(ThemeManager.APP_BG);
        setBorder(ThemeManager.panelPadding(24, 24, 24, 24));

        add(createHeroPanel(), BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new BorderLayout(24, 24));
        contentPanel.setOpaque(false);

        totalServicesCard = new MetricCard("Services", "Registered endpoints", "\u25A6", ThemeManager.INFO);
        totalExperimentsCard = new MetricCard("Experiments", "Created chaos plans", "\u2697", ThemeManager.ACCENT_SOFT);
        activeRunsCard = new MetricCard("Active Runs", "Currently executing", "\u25B6", ThemeManager.WARNING);
        avgResilienceCard = new MetricCard("Resilience", "Average platform score", "\u2605", ThemeManager.ACCENT);
        healthyCountCard = new MetricCard("Healthy", "Stable services", "\u2713", ThemeManager.SUCCESS);
        degradedCountCard = new MetricCard("Degraded", "Needs attention", "\u26A0", ThemeManager.WARNING);
        downCountCard = new MetricCard("Down", "Immediate action", "\u2715", ThemeManager.DANGER);

        contentPanel.add(createMetricsGrid(), BorderLayout.WEST);

        topologyPanel = new TopologyGraphPanel();
        contentPanel.add(topologyPanel, BorderLayout.CENTER);

        add(contentPanel, BorderLayout.CENTER);

        refresh();
        new Timer(5000, e -> refresh()).start();
    }

    private JPanel createHeroPanel() {
        CardPanel hero = new CardPanel(new Color(17, 29, 54), new Color(66, 102, 168), 32);
        hero.setLayout(new BorderLayout(18, 18));
        hero.setBorder(ThemeManager.panelPadding(24, 28, 24, 28));

        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));

        JLabel eyebrow = new JLabel("MICROCHAOS CONTROL CENTER");
        eyebrow.setFont(ThemeManager.font(12, Font.BOLD));
        eyebrow.setForeground(ThemeManager.ACCENT_SOFT);

        JLabel title = new JLabel("Operational overview for your chaos engineering stack");
        title.setFont(ThemeManager.font(28, Font.BOLD));
        title.setForeground(ThemeManager.TEXT_PRIMARY);

        JLabel subtitle = new JLabel("Track service health, resilience posture, and topology state from one place.");
        subtitle.setFont(ThemeManager.font(15, Font.PLAIN));
        subtitle.setForeground(ThemeManager.TEXT_MUTED);

        copy.add(eyebrow);
        copy.add(Box.createVerticalStrut(8));
        copy.add(title);
        copy.add(Box.createVerticalStrut(8));
        copy.add(subtitle);

        JButton refreshButton = createRefreshButton();

        hero.add(copy, BorderLayout.CENTER);
        hero.add(refreshButton, BorderLayout.EAST);
        return hero;
    }

    private JButton createRefreshButton() {
        JButton button = new JButton("Refresh Now");
        button.setFocusPainted(false);
        button.setBackground(ThemeManager.ACCENT);
        button.setForeground(ThemeManager.TEXT_PRIMARY);
        button.setFont(ThemeManager.font(13, Font.BOLD));
        button.setMargin(new Insets(12, 18, 12, 18));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(142, 197, 255), 1, true),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        button.addActionListener(e -> refresh());
        return button;
    }

    private JPanel createMetricsGrid() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setPreferredSize(new Dimension(380, 0));

        JPanel grid = new JPanel(new GridLayout(0, 2, 18, 18));
        grid.setOpaque(false);
        grid.add(totalServicesCard);
        grid.add(totalExperimentsCard);
        grid.add(activeRunsCard);
        grid.add(avgResilienceCard);
        grid.add(healthyCountCard);
        grid.add(degradedCountCard);
        grid.add(downCountCard);
        grid.add(createStatusLegendCard());

        wrapper.add(grid, BorderLayout.NORTH);
        return wrapper;
    }

    private JPanel createStatusLegendCard() {
        CardPanel card = new CardPanel(ThemeManager.SURFACE_ALT, ThemeManager.BORDER, 28);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(ThemeManager.panelPadding(18, 18, 18, 18));

        JLabel title = new JLabel("Status Legend");
        title.setFont(ThemeManager.font(16, Font.BOLD));
        title.setForeground(ThemeManager.TEXT_PRIMARY);
        card.add(title);
        card.add(Box.createVerticalStrut(14));
        card.add(createLegendRow("Healthy", ThemeManager.SUCCESS));
        card.add(Box.createVerticalStrut(10));
        card.add(createLegendRow("Degraded", ThemeManager.WARNING));
        card.add(Box.createVerticalStrut(10));
        card.add(createLegendRow("Down", ThemeManager.DANGER));
        card.add(Box.createVerticalStrut(10));
        card.add(createLegendRow("Managed", ThemeManager.ACCENT_SOFT));
        return card;
    }

    private JPanel createLegendRow(String labelText, Color color) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);

        JPanel dot = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillOval(2, 2, 12, 12);
                g2.dispose();
            }
        };
        dot.setOpaque(false);
        dot.setPreferredSize(new Dimension(16, 16));

        JLabel label = new JLabel(labelText);
        label.setFont(ThemeManager.font(13, Font.PLAIN));
        label.setForeground(ThemeManager.TEXT_MUTED);

        row.add(dot, BorderLayout.WEST);
        row.add(label, BorderLayout.CENTER);
        return row;
    }

    private void refresh() {
        new Thread(() -> {
            try {
                DashboardOverview overview = apiClient.get("/dashboard/overview", DashboardOverview.class);
                List<Service> services = apiClient.getList("/services", Service.class);

                SwingUtilities.invokeLater(() -> {
                    totalServicesCard.setValue(String.valueOf(overview.totalServices));
                    totalExperimentsCard.setValue(String.valueOf(overview.totalExperiments));
                    activeRunsCard.setValue(String.valueOf(overview.activeRuns));
                    avgResilienceCard.setValue(String.format("%.1f", overview.averageResilienceScore));

                    if (overview.monitoring != null) {
                        healthyCountCard.setValue(String.valueOf(overview.monitoring.healthyCount));
                        degradedCountCard.setValue(String.valueOf(overview.monitoring.degradedCount));
                        downCountCard.setValue(String.valueOf(overview.monitoring.downCount));
                    } else {
                        healthyCountCard.setValue("--");
                        degradedCountCard.setValue("--");
                        downCountCard.setValue("--");
                    }

                    topologyPanel.updateServices(services);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "dashboard-refresh").start();
    }

    private static class MetricCard extends CardPanel {
        private final JLabel valueLabel;

        MetricCard(String title, String subtitle, String icon, Color accent) {
            super(ThemeManager.SURFACE, accent.darker(), 28);
            setLayout(new BorderLayout(0, 14));
            setBorder(ThemeManager.panelPadding(18, 18, 18, 18));
            setPreferredSize(new Dimension(170, 128));

            JPanel topRow = new JPanel(new BorderLayout());
            topRow.setOpaque(false);

            JPanel iconPill = new JPanel(new BorderLayout());
            iconPill.setOpaque(true);
            iconPill.setBackground(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 55));
            iconPill.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 110), 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
            ));

            JLabel iconLabel = new JLabel(icon, SwingConstants.CENTER);
            iconLabel.setForeground(accent);
            iconLabel.setFont(ThemeManager.font(15, Font.BOLD));
            iconPill.add(iconLabel, BorderLayout.CENTER);

            JLabel titleLabel = new JLabel(title);
            titleLabel.setForeground(ThemeManager.TEXT_PRIMARY);
            titleLabel.setFont(ThemeManager.font(15, Font.BOLD));

            valueLabel = new JLabel("--");
            valueLabel.setForeground(ThemeManager.TEXT_PRIMARY);
            valueLabel.setFont(ThemeManager.font(28, Font.BOLD));

            JLabel subtitleLabel = new JLabel(subtitle);
            subtitleLabel.setForeground(ThemeManager.TEXT_MUTED);
            subtitleLabel.setFont(ThemeManager.font(12, Font.PLAIN));

            topRow.add(titleLabel, BorderLayout.WEST);
            topRow.add(iconPill, BorderLayout.EAST);

            JPanel bottom = new JPanel();
            bottom.setOpaque(false);
            bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
            bottom.add(valueLabel);
            bottom.add(Box.createVerticalStrut(6));
            bottom.add(subtitleLabel);

            add(topRow, BorderLayout.NORTH);
            add(bottom, BorderLayout.CENTER);
        }

        void setValue(String value) {
            valueLabel.setText(value);
        }
    }

    private static class TopologyGraphPanel extends CardPanel {
        private List<Service> services = new ArrayList<>();

        TopologyGraphPanel() {
            super(ThemeManager.SURFACE, ThemeManager.BORDER, 32);
            setLayout(new BorderLayout());
            setBorder(ThemeManager.panelPadding(18, 18, 18, 18));
            setPreferredSize(new Dimension(700, 540));
        }

        void updateServices(List<Service> services) {
            this.services = services == null ? new ArrayList<>() : services;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int innerWidth = getWidth() - 36;
            int innerHeight = getHeight() - 42;
            int originX = 18;
            int originY = 18;

            GradientPaint bg = new GradientPaint(
                originX,
                originY,
                new Color(19, 30, 52),
                originX + innerWidth,
                originY + innerHeight,
                new Color(12, 20, 36)
            );
            g2.setPaint(bg);
            g2.fill(new RoundRectangle2D.Double(originX, originY, innerWidth, innerHeight, 28, 28));

            g2.setColor(new Color(77, 104, 154, 70));
            g2.draw(new RoundRectangle2D.Double(originX, originY, innerWidth, innerHeight, 28, 28));

            g2.setFont(ThemeManager.font(18, Font.BOLD));
            g2.setColor(ThemeManager.TEXT_PRIMARY);
            g2.drawString("Topology Graph", 34, 44);

            g2.setFont(ThemeManager.font(12, Font.PLAIN));
            g2.setColor(ThemeManager.TEXT_MUTED);
            g2.drawString("Live service map seeded from backend service registry", 34, 66);

            if (services == null || services.isEmpty()) {
                g2.setColor(ThemeManager.TEXT_MUTED);
                g2.setFont(ThemeManager.font(16, Font.PLAIN));
                g2.drawString("No services loaded yet.", 34, getHeight() / 2);
                g2.dispose();
                return;
            }

            int centerX = getWidth() / 2;
            int centerY = getHeight() / 2 + 14;
            int orbitRadius = Math.min(getWidth(), getHeight()) / 3;

            drawHub(g2, centerX, centerY);

            List<PointNode> nodes = new ArrayList<>();
            for (int i = 0; i < services.size(); i++) {
                double angle = (2 * Math.PI * i) / services.size() - Math.PI / 2;
                int x = (int) (centerX + orbitRadius * Math.cos(angle));
                int y = (int) (centerY + orbitRadius * Math.sin(angle));
                nodes.add(new PointNode(services.get(i), x, y));
            }

            g2.setStroke(new BasicStroke(1.4f));
            for (PointNode node : nodes) {
                g2.setColor(new Color(90, 124, 187, 110));
                g2.draw(new Line2D.Double(centerX, centerY, node.x, node.y));
            }

            for (PointNode node : nodes) {
                drawNode(g2, node);
            }

            g2.dispose();
        }

        private void drawHub(Graphics2D g2, int centerX, int centerY) {
            float[] fractions = {0f, 1f};
            Color[] colors = {new Color(78, 163, 255, 180), new Color(78, 163, 255, 10)};
            RadialGradientPaint glow = new RadialGradientPaint(centerX, centerY, 95, fractions, colors);
            g2.setPaint(glow);
            g2.fill(new Ellipse2D.Double(centerX - 95, centerY - 95, 190, 190));

            g2.setColor(new Color(32, 52, 92));
            g2.fill(new Ellipse2D.Double(centerX - 44, centerY - 44, 88, 88));
            g2.setColor(ThemeManager.ACCENT_SOFT);
            g2.setStroke(new BasicStroke(2.2f));
            g2.draw(new Ellipse2D.Double(centerX - 44, centerY - 44, 88, 88));

            g2.setColor(ThemeManager.TEXT_PRIMARY);
            g2.setFont(ThemeManager.font(14, Font.BOLD));
            g2.drawString("CORE", centerX - 18, centerY + 5);
        }

        private void drawNode(Graphics2D g2, PointNode node) {
            Color fill = resolveStatusColor(node.service.status);
            int radius = 34;

            g2.setColor(new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 40));
            g2.fill(new Ellipse2D.Double(node.x - radius - 7, node.y - radius - 7, (radius + 7) * 2.0, (radius + 7) * 2.0));

            g2.setColor(new Color(20, 31, 52));
            g2.fill(new Ellipse2D.Double(node.x - radius, node.y - radius, radius * 2.0, radius * 2.0));

            g2.setColor(fill);
            g2.setStroke(new BasicStroke(2.0f));
            g2.draw(new Ellipse2D.Double(node.x - radius, node.y - radius, radius * 2.0, radius * 2.0));

            g2.setFont(ThemeManager.font(11, Font.BOLD));
            g2.setColor(ThemeManager.TEXT_PRIMARY);
            String name = abbreviate(node.service.name, 14);
            int textWidth = g2.getFontMetrics().stringWidth(name);
            g2.drawString(name, node.x - textWidth / 2, node.y + 4);

            g2.setFont(ThemeManager.font(10, Font.PLAIN));
            g2.setColor(ThemeManager.TEXT_MUTED);
            String env = abbreviate(node.service.environment == null ? "unknown" : node.service.environment, 10);
            int envWidth = g2.getFontMetrics().stringWidth(env);
            g2.drawString(env, node.x - envWidth / 2, node.y + 20);
        }

        private Color resolveStatusColor(String status) {
            if ("DOWN".equalsIgnoreCase(status)) {
                return ThemeManager.DANGER;
            }
            if ("DEGRADED".equalsIgnoreCase(status)) {
                return ThemeManager.WARNING;
            }
            return ThemeManager.SUCCESS;
        }

        private String abbreviate(String value, int maxLength) {
            if (value == null) {
                return "";
            }
            return value.length() <= maxLength ? value : value.substring(0, maxLength - 1) + "\u2026";
        }

        private static class PointNode {
            private final Service service;
            private final int x;
            private final int y;

            private PointNode(Service service, int x, int y) {
                this.service = service;
                this.x = x;
                this.y = y;
            }
        }
    }
}
