package com.microchaos.swing.ui.theme;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.border.Border;

public final class ThemeManager {
    public static final Color APP_BG = new Color(11, 18, 32);
    public static final Color SURFACE = new Color(18, 26, 44);
    public static final Color SURFACE_ALT = new Color(24, 35, 58);
    public static final Color SURFACE_ELEVATED = new Color(31, 45, 72);
    public static final Color BORDER = new Color(50, 72, 112);
    public static final Color TEXT_PRIMARY = new Color(237, 242, 255);
    public static final Color TEXT_MUTED = new Color(148, 163, 184);
    public static final Color ACCENT = new Color(78, 163, 255);
    public static final Color ACCENT_SOFT = new Color(104, 195, 255);
    public static final Color SUCCESS = new Color(34, 197, 94);
    public static final Color WARNING = new Color(245, 158, 11);
    public static final Color DANGER = new Color(239, 68, 68);
    public static final Color INFO = new Color(59, 130, 246);

    private static final String PRIMARY_FONT_FAMILY = resolveFontFamily();

    private ThemeManager() {}

    public static void applyGlobalTheme() {
        Font regular = font(14, Font.PLAIN);
        Font bold = font(14, Font.BOLD);

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Keep Swing usable even if the system look and feel is unavailable.
        }

        UIDefaults defaults = UIManager.getDefaults();
        defaults.put("Panel.background", APP_BG);
        defaults.put("Label.foreground", TEXT_PRIMARY);
        defaults.put("Button.font", bold);
        defaults.put("Label.font", regular);
        defaults.put("TabbedPane.font", font(14, Font.BOLD));
        defaults.put("TabbedPane.background", SURFACE);
        defaults.put("TabbedPane.foreground", TEXT_MUTED);
        defaults.put("TabbedPane.selected", SURFACE_ALT);
        defaults.put("TabbedPane.contentAreaColor", APP_BG);
        defaults.put("TabbedPane.focus", APP_BG);
        defaults.put("Table.background", SURFACE);
        defaults.put("Table.foreground", TEXT_PRIMARY);
        defaults.put("Table.selectionBackground", new Color(37, 99, 235));
        defaults.put("Table.selectionForeground", TEXT_PRIMARY);
        defaults.put("Table.gridColor", BORDER);
        defaults.put("TableHeader.background", SURFACE_ALT);
        defaults.put("TableHeader.foreground", TEXT_PRIMARY);
        defaults.put("TextField.background", SURFACE_ALT);
        defaults.put("TextField.foreground", TEXT_PRIMARY);
        defaults.put("TextField.caretForeground", TEXT_PRIMARY);
    }

    public static Font font(int size, int style) {
        return new Font(PRIMARY_FONT_FAMILY, style, size);
    }

    public static Border panelPadding(int top, int left, int bottom, int right) {
        return BorderFactory.createEmptyBorder(top, left, bottom, right);
    }

    public static Border compoundCardBorder() {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1, true),
            BorderFactory.createEmptyBorder(18, 18, 18, 18)
        );
    }

    private static String resolveFontFamily() {
        Set<String> families = Set.of(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        if (families.contains("Segoe UI")) {
            return "Segoe UI";
        }
        if (families.contains("Inter")) {
            return "Inter";
        }
        return "SansSerif";
    }
}
