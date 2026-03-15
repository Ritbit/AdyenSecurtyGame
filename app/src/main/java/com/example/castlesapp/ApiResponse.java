package com.example.castlesapp;

import android.graphics.Bitmap;

/**
 * Parsed response from POST /api/secgame.
 *
 * The backend is the single source of truth for all lockout state.
 * The app never counts attempts itself — it just renders what this object says.
 *
 * Possible states:
 *
 *   status = OK       → image is set; print it
 *   status = INVALID  → wrong code; attemptsRemaining tells the user how many are left
 *                       lockoutSeconds == 0 means no lockout yet
 *   status = LOCKED   → pad must be disabled; lockoutSeconds is the remaining wait
 *   status = ERROR    → message explains what went wrong (network / server)
 */
public class ApiResponse {

    public enum Status { OK, INVALID, LOCKED, ERROR }

    public final Status status;

    /** Set when status == OK. The decoded bitmap ready to print. */
    public final Bitmap image;

    /** Set when status == INVALID. How many more tries before lockout. */
    public final int attemptsRemaining;

    /**
     * Set when status == INVALID or LOCKED.
     * 0 = not locked (just a wrong code warning).
     * >0 = locked; this many seconds must elapse before the pad re-enables.
     */
    public final int lockoutSeconds;

    /** Set when status == ERROR. Human-readable description. */
    public final String message;

    // ── Private constructors — use factory methods below ─────────────────────

    private ApiResponse(Status status, Bitmap image,
                        int attemptsRemaining, int lockoutSeconds, String message) {
        this.status            = status;
        this.image             = image;
        this.attemptsRemaining = attemptsRemaining;
        this.lockoutSeconds    = lockoutSeconds;
        this.message           = message;
    }

    public static ApiResponse ok(Bitmap image) {
        return new ApiResponse(Status.OK, image, 0, 0, null);
    }

    public static ApiResponse invalid(int attemptsRemaining, int lockoutSeconds) {
        return new ApiResponse(Status.INVALID, null, attemptsRemaining, lockoutSeconds, null);
    }

    public static ApiResponse locked(int lockoutSeconds) {
        return new ApiResponse(Status.LOCKED, null, 0, lockoutSeconds, null);
    }

    public static ApiResponse error(String message) {
        return new ApiResponse(Status.ERROR, null, 0, 0, message);
    }
}
