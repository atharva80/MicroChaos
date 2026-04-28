package com.microchaos.swing.ui.theme;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

/**
 * Utility class for styling JTable components with a consistent dark theme.
 * Apply this styling to any JTable for header visibility and striped rows.
 */
public final class TableStyler {

    // Table colors
    public static final Color HEADER_BG = new Color(20, 32, 55);
    public static final Color HEADER_TEXT = Color.WHITE;
    public static final Color ROW_EVEN = new Color(18, 26, 44);
    public static final Color ROW_ODD = new Color(24, 35, 58);
    public static final Color ROW_SELECTED = new Color(37, 99, 235);
    public static final Color ROW_HOVER = new Color(40, 60, 100);
    public static final Color TEXT_COLOR = new Color(237, 242, 255);
    public static final Color BORDER_COLOR = new Color(50, 72, 112);

    // Button colors
    public static final Color BUTTON_BG = new Color(40, 60, 120);
    public static final Color BUTTON_TEXT = Color.WHITE;
    public static final Color BUTTON_HOVER = new Color(60, 100, 180);

    private TableStyler() {}

    /**
     * Apply complete table styling to any JTable.
     * Call this method after creating your JTable.
     *
     * @param table the JTable to style
     */
    public static void styleTable(JTable table) {
        if (table == null) return;

        styleTableHeader(table);
        styleTableContent(table);
        styleTableDefaults(table);
    }

    /**
     * Style the table header: dark background, white text, bold font.
     * PROPERLY overrides the default JTableHeader renderer.
     */
    public static void styleTableHeader(JTable table) {
        JTableHeader header = table.getTableHeader();
        if (header == null) return;

        // Set header properties
        header.setBackground(HEADER_BG);
        header.setForeground(HEADER_TEXT);
        header.setFont(new Font("Arial", Font.BOLD, 14));
        header.setReorderingAllowed(false);
        header.setResizingAllowed(true);
        header.setOpaque(true);

        // CRITICAL: Override the default header renderer
        header.setDefaultRenderer(new CustomHeaderRenderer());

        // Set row renderer with striped rows
        table.setDefaultRenderer(Object.class, new StripedTableRenderer());
    }

    /**
     * Custom header renderer - properly draws dark background with white text.
     */
    private static class CustomHeaderRenderer extends DefaultTableCellRenderer {
        public CustomHeaderRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            setFont(new Font("Arial", Font.BOLD, 14));
            setText(value != null ? value.toString() : "");
            setForeground(HEADER_TEXT);
            setBackground(HEADER_BG);
            setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, BORDER_COLOR));
            setOpaque(true);

            return this;
        }
    }

    /**
     * Style table content: larger font, better row height, striped rows.
     */
    public static void styleTableContent(JTable table) {
        // Increase font size by 1 level (14 + 1 = 15)
        table.setFont(new Font("Arial", Font.PLAIN, 15));

        // Improve row height for readability
        table.setRowHeight(28);

        // Enable row selection
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(false);

        // Set inter-cell spacing
        table.setIntercellSpacing(new java.awt.Dimension(0, 1));

        // Grid color
        table.setGridColor(BORDER_COLOR);
    }

    /**
     * Set additional table defaults.
     */
    public static void styleTableDefaults(JTable table) {
        table.setBackground(ROW_EVEN);
        table.setForeground(TEXT_COLOR);
        table.setSelectionBackground(ROW_SELECTED);
        table.setSelectionForeground(Color.WHITE);
        table.setOpaque(true);
        table.setBorder(null);
    }

    /**
     * Custom renderer for striped rows with hover effect.
     */
    private static class StripedTableRenderer extends DefaultTableCellRenderer {
        private int lastRow = -1;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (isSelected) {
                setBackground(ROW_SELECTED);
                setForeground(Color.WHITE);
            } else {
                // Alternating row colors (striped rows)
                setBackground(row % 2 == 0 ? ROW_EVEN : ROW_ODD);
                setForeground(TEXT_COLOR);
            }

            setFont(new Font("Arial", Font.PLAIN, 14));
            setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            setOpaque(true);

            return this;
        }
    }

    /**
     * Apply styling to a table with custom row height.
     *
     * @param table the JTable to style
     * @param rowHeight custom row height value
     */
    public static void styleTable(JTable table, int rowHeight) {
        styleTable(table);
        table.setRowHeight(rowHeight);
    }

    /**
     * Apply styling with custom font size.
     *
     * @param table the JTable to style
     * @param fontSize custom font size
     */
    public static void styleTable(JTable table, int rowHeight, int fontSize) {
        styleTable(table);
        table.setRowHeight(rowHeight);
        table.setFont(new Font("Arial", Font.PLAIN, fontSize));
    }
}