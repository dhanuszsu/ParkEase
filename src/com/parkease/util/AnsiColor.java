package com.parkease.util;

public class AnsiColor {
    public static final String RESET = "\u001B[0m";
    public static final String BOLD = "\u001B[1m";
    public static final String DIM = "\u001B[2m";

    public static final String ORANGE = "\u001B[38;2;217;119;87m";
    public static final String MUTED_ORANGE = "\u001B[38;2;180;100;75m";
    public static final String WHITE = "\u001B[97m";
    public static final String GRAY = "\u001B[90m";

    private static boolean enabled = true;

    public static void setEnabled(boolean isEnabled) {
        enabled = isEnabled;
    }

    public static String color(String code, String text) {
        if (!enabled) return text;
        return code + text + RESET;
    }

    public static String orange(String text) { return color(ORANGE, text); }
    public static String mutedOrange(String text) { return color(MUTED_ORANGE, text); }
    public static String white(String text) { return color(WHITE, text); }
    public static String gray(String text) { return color(GRAY, text); }
    public static String bold(String text) { return color(BOLD, text); }
    public static String dim(String text) { return color(DIM, text); }

    public static String green(String text) { return color(WHITE, text); }
    public static String red(String text) { return color(ORANGE, text); }
    public static String yellow(String text) { return color(ORANGE, text); }
    public static String blue(String text) { return color(WHITE, text); }
    public static String magenta(String text) { return color(ORANGE, text); }
    public static String cyan(String text) { return color(ORANGE, text); }
    public static String lightOrange(String text) { return color(ORANGE, text); }

    public static String orangeGradientText(String text) {
        if (!enabled) return text;
        String[] shades = {
            "\u001B[38;2;190;95;65m",
            "\u001B[38;2;217;119;87m",
            "\u001B[38;2;235;135;105m",
            "\u001B[38;2;217;119;87m",
            "\u001B[38;2;190;95;65m"
        };
        StringBuilder sb = new StringBuilder();
        int len = text.length();
        for (int i = 0; i < len; i++) {
            int idx = (int) (((double) i / len) * shades.length);
            if (idx >= shades.length) idx = shades.length - 1;
            sb.append(shades[idx]).append(text.charAt(i));
        }
        sb.append(RESET);
        return sb.toString();
    }

    public static String gradientText(String text) {
        return orangeGradientText(text);
    }

    public static void showSpinner(String message, int durationMs) {
        char[] spinner = {'⠋', '⠙', '⠹', '⠸', '⠼', '⠴', '⠦', '⠧', '⠇', '⠏'};
        int steps = durationMs / 80;
        for (int i = 0; i < steps; i++) {
            char frame = spinner[i % spinner.length];
            System.out.print("\r" + orange(String.valueOf(frame)) + " " + white(message));
            System.out.flush();
            try {
                Thread.sleep(80);
            } catch (InterruptedException ignored) {}
        }
        System.out.print("\r" + orange("✔") + " " + white(message) + " " + orange("[DONE]\n"));
        System.out.flush();
    }

    public static void showProgressBar(String taskName, int durationMs) {
        int totalBars = 30;
        int steps = durationMs / 40;
        for (int i = 0; i <= steps; i++) {
            double percent = (double) i / steps;
            int filled = (int) (percent * totalBars);
            int empty = totalBars - filled;
            String bar = "█".repeat(filled) + "░".repeat(empty);
            System.out.print("\r" + orange(taskName) + " [" + orangeGradientText(bar) + orange(String.format("] %3.0f%%", percent * 100)));
            System.out.flush();
            try {
                Thread.sleep(40);
            } catch (InterruptedException ignored) {}
        }
        System.out.println();
    }
}
