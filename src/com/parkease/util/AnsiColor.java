package com.parkease.util;

/**
 * Utility providing ANSI escape codes for professional, rich CLI rendering.
 */
public class AnsiColor {
    public static final String RESET = "\u001B[0m";
    public static final String BOLD = "\u001B[1m";
    public static final String DIM = "\u001B[2m";
    public static final String UNDERLINE = "\u001B[4m";

    // Regular Colors
    public static final String BLACK = "\u001B[30m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String MAGENTA = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";

    // Bright Colors
    public static final String BRIGHT_RED = "\u001B[91m";
    public static final String BRIGHT_GREEN = "\u001B[92m";
    public static final String BRIGHT_YELLOW = "\u001B[93m";
    public static final String BRIGHT_BLUE = "\u001B[94m";
    public static final String BRIGHT_MAGENTA = "\u001B[95m";
    public static final String BRIGHT_CYAN = "\u001B[96m";
    public static final String BRIGHT_WHITE = "\u001B[97m";

    // Backgrounds
    public static final String BG_GREEN = "\u001B[42m";
    public static final String BG_YELLOW = "\u001B[43m";
    public static final String BG_BLUE = "\u001B[44m";
    public static final String BG_MAGENTA = "\u001B[45m";
    public static final String BG_CYAN = "\u001B[46m";
    public static final String BG_DARK_GRAY = "\u001B[100m";

    private static boolean enabled = true;

    public static void setEnabled(boolean isEnabled) {
        enabled = isEnabled;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static String color(String code, String text) {
        if (!enabled) return text;
        return code + text + RESET;
    }

    public static String green(String text) { return color(BRIGHT_GREEN, text); }
    public static String red(String text) { return color(BRIGHT_RED, text); }
    public static String yellow(String text) { return color(BRIGHT_YELLOW, text); }
    public static String blue(String text) { return color(BRIGHT_BLUE, text); }
    public static String magenta(String text) { return color(BRIGHT_MAGENTA, text); }
    public static String cyan(String text) { return color(BRIGHT_CYAN, text); }
    public static String bold(String text) { return color(BOLD, text); }
    public static String white(String text) { return color(BRIGHT_WHITE, text); }
}
