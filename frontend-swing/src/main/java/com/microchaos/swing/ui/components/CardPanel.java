package com.microchaos.swing.ui.components;

import com.microchaos.swing.ui.theme.ThemeManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JPanel;

public class CardPanel extends JPanel {
    private final Color backgroundColor;
    private final Color borderColor;
    private final int arc;

    public CardPanel() {
        this(ThemeManager.SURFACE, ThemeManager.BORDER, 28);
    }

    public CardPanel(Color backgroundColor, Color borderColor, int arc) {
        this.backgroundColor = backgroundColor;
        this.borderColor = borderColor;
        this.arc = arc;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int shadowOffset = 6;
        g2.setColor(new Color(2, 6, 23, 110));
        g2.fill(new RoundRectangle2D.Double(3, shadowOffset, getWidth() - 6, getHeight() - shadowOffset - 2, arc, arc));

        g2.setColor(backgroundColor);
        g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - shadowOffset, arc, arc));

        g2.setColor(borderColor);
        g2.setStroke(new BasicStroke(1.15f));
        g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 2.0, getHeight() - shadowOffset - 1.0, arc, arc));
        g2.dispose();

        super.paintComponent(g);
    }
}
