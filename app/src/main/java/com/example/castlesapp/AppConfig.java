package com.example.castlesapp;

/**
 * Central configuration for the app.
 * Edit this file before building — no other file needs touching for deployment.
 */
public final class AppConfig {

    private AppConfig() {}

    // -------------------------------------------------------------------------
    // Backend
    // -------------------------------------------------------------------------

    /**
     * Base URL of the Adyen Security Game backend.
     * Set in build.gradle productFlavors — both mock and prod use:
     *   https://adyen.ritbit.net/api/secgame
     */
    public static final String BASE_URL = BuildConfig.BACKEND_BASE_URL;

    /** Validate endpoint — full URL = BASE_URL (the path is already included). */
    public static final String VALIDATE_PATH = "";

    /**
     * Identifies this physical terminal to the backend.
     * The backend uses this to track attempts and lockouts per device,
     * and to allow targeted resets via the admin API.
     *
     * Change this per terminal if you deploy multiple S1F2 units.
     * You could also derive it from Settings.Secure.ANDROID_ID at runtime.
     */
    public static final String TERMINAL_ID = "terminal-01";

    /** HTTP connect timeout in seconds. */
    public static final int CONNECT_TIMEOUT_S = 15;

    /** HTTP read timeout in seconds (increase if the server generates the image on the fly). */
    public static final int READ_TIMEOUT_S = 30;

    // -------------------------------------------------------------------------
    // PIN / UX
    // -------------------------------------------------------------------------

    /** Number of digits in the PIN (default 4). */
    public static final int PIN_LENGTH = 4;

    /**
     * Milliseconds to display the "Printed!" success message before
     * resetting the pad for the next user.
     */
    public static final int SUCCESS_RESET_DELAY_MS = 2500;

    /**
     * Milliseconds to display a print-error message before auto-resetting.
     */
    public static final int ERROR_RESET_DELAY_MS = 2000;

    // -------------------------------------------------------------------------
    // Printer
    // -------------------------------------------------------------------------

    /**
     * Castles S1F2 thermal head width in pixels.
     * 58 mm head at 203 dpi = 384 px.
     * Bitmaps wider than this are scaled down automatically.
     */
    public static final int PRINTER_WIDTH_PX = 384;
}
