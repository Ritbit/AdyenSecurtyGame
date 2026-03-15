package com.example.castlesapp;

import android.os.SystemClock;
import android.util.Log;

/**
 * Client-side brute-force protection for the PIN pad.
 *
 * After MAX_ATTEMPTS consecutive wrong codes the pad is locked for an
 * escalating cooldown period.  The counter resets on a successful print.
 *
 * Lockout schedule (configurable via constants below):
 *   Attempt 5  →  30 s
 *   Attempt 6  →  60 s
 *   Attempt 7  →  120 s
 *   Attempt 8+ →  300 s  (5 min, capped)
 *
 * NOTE: This is a UI-layer safeguard only.  Your backend should enforce
 * its own rate-limiting (e.g. per-IP or per-terminal token bucket).
 */
public class PinLockout {

    private static final String TAG = "PinLockout";

    // ─── Configuration ────────────────────────────────────────────────────────
    private static final int  MAX_ATTEMPTS          = 4;   // free attempts before first lock
    private static final long BASE_LOCKOUT_MS       = 30_000L;   // 30 s first lockout
    private static final long MAX_LOCKOUT_MS        = 300_000L;  // 5 min cap
    // ──────────────────────────────────────────────────────────────────────────

    private int  failCount       = 0;
    private long lockedUntilMs   = 0;   // epoch ms from SystemClock.elapsedRealtime()

    /** Call after every rejected code (HTTP 401/403/404). */
    public void recordFailure() {
        failCount++;
        if (failCount > MAX_ATTEMPTS) {
            // Each extra failure doubles the lockout, capped at MAX_LOCKOUT_MS
            int extra = failCount - MAX_ATTEMPTS;          // 1, 2, 3 …
            long duration = Math.min(BASE_LOCKOUT_MS * (1L << (extra - 1)), MAX_LOCKOUT_MS);
            lockedUntilMs = SystemClock.elapsedRealtime() + duration;
            Log.w(TAG, "Locked out for " + (duration / 1000) + "s after " + failCount + " failures");
        }
    }

    /** Call after a successful print — resets everything. */
    public void recordSuccess() {
        failCount     = 0;
        lockedUntilMs = 0;
        Log.d(TAG, "Lockout counter reset after success");
    }

    /** @return true if the pad is currently locked. */
    public boolean isLocked() {
        return SystemClock.elapsedRealtime() < lockedUntilMs;
    }

    /**
     * Remaining lockout time in whole seconds (0 if not locked).
     * Use this to show a countdown to the user.
     */
    public int remainingSeconds() {
        long remaining = lockedUntilMs - SystemClock.elapsedRealtime();
        return remaining > 0 ? (int) Math.ceil(remaining / 1000.0) : 0;
    }

    /** Number of consecutive failures so far. */
    public int failureCount() {
        return failCount;
    }

    /**
     * How many more wrong attempts before the first lockout triggers.
     * Returns 0 once the account is already in lockout.
     */
    public int attemptsRemaining() {
        return Math.max(0, MAX_ATTEMPTS - failCount);
    }
}
