package com.example.castlesapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.castlesapp.databinding.ActivityMainBinding;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main screen — PIN entry, backend call, print, backend-controlled lockout.
 *
 * The app contains ZERO lockout logic of its own.
 * Every decision (invalid / locked / how long) comes from the backend response.
 *
 * Flow:
 *  1. User taps digits. After PIN_LENGTH digits the backend is called.
 *  2. status=ok      → PrintResultActivity (success, green)
 *  3. status=invalid → show "X attempts remaining"; if lockoutSeconds>0 start countdown
 *  4. status=locked  → show countdown; pad disabled until seconds reach 0
 *  5. status=error   → inline message; pad re-enabled immediately
 *  6. onDestroy      → all Runnables cancelled, executor interrupted
 */
public class MainActivity extends AppCompatActivity {

    private static final int PIN_LENGTH = AppConfig.PIN_LENGTH;

    // ── State ─────────────────────────────────────────────────────────────────
    private final StringBuilder pin = new StringBuilder();

    // ── Threading ─────────────────────────────────────────────────────────────
    private final ExecutorService executor  = Executors.newSingleThreadExecutor();
    private final Handler         uiHandler = new Handler(Looper.getMainLooper());

    private Runnable countdownRunnable;
    private Runnable resetRunnable;

    // Countdown target: System.currentTimeMillis() + lockoutSeconds * 1000
    private long lockedUntilMs = 0;

    // ── View binding ──────────────────────────────────────────────────────────
    private ActivityMainBinding b;
    private View[] dots;

    // =========================================================================
    // Lifecycle
    // =========================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b    = ActivityMainBinding.inflate(getLayoutInflater());
        dots = new View[]{ b.dot1, b.dot2, b.dot3, b.dot4 };
        setContentView(b.getRoot());
        setupKeypad();
        refreshDots();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Returning from PrintResultActivity — re-check lockout in case
        // the timer expired while the result screen was showing.
        if (!isLocked()) {
            resetPin();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopCountdown();
        cancelReset();
        executor.shutdownNow();
    }

    // =========================================================================
    // Keypad
    // =========================================================================

    private void setupKeypad() {
        b.btn0.setOnClickListener(v -> appendDigit("0"));
        b.btn1.setOnClickListener(v -> appendDigit("1"));
        b.btn2.setOnClickListener(v -> appendDigit("2"));
        b.btn3.setOnClickListener(v -> appendDigit("3"));
        b.btn4.setOnClickListener(v -> appendDigit("4"));
        b.btn5.setOnClickListener(v -> appendDigit("5"));
        b.btn6.setOnClickListener(v -> appendDigit("6"));
        b.btn7.setOnClickListener(v -> appendDigit("7"));
        b.btn8.setOnClickListener(v -> appendDigit("8"));
        b.btn9.setOnClickListener(v -> appendDigit("9"));

        b.btnBack.setOnClickListener(v -> {
            if (pin.length() > 0) {
                pin.deleteCharAt(pin.length() - 1);
                refreshDots();
                clearStatus();
            }
        });

        b.btnClear.setOnClickListener(v -> resetPin());
    }

    private void appendDigit(String digit) {
        if (isLocked()) return;
        if (pin.length() >= PIN_LENGTH) return;
        clearStatus();
        pin.append(digit);
        refreshDots();
        if (pin.length() == PIN_LENGTH) onPinComplete();
    }

    // =========================================================================
    // PIN complete → call backend
    // =========================================================================

    private void onPinComplete() {
        setInputEnabled(false);
        showLoading(true);
        clearStatus();

        final String code = pin.toString();

        executor.execute(() -> RetryApiClient.validateCode(code, response -> {
            uiHandler.post(() -> {
                showLoading(false);
                handleResponse(response);
            });
        }));
    }

    // =========================================================================
    // Response handler — all lockout logic driven by backend values
    // =========================================================================

    private void handleResponse(ApiResponse r) {
        switch (r.status) {

            case OK:
                // Success — clear any lockout, launch result screen
                lockedUntilMs = 0;
                stopCountdown();
                PrinterManager.printBitmap(r.image, new PrinterManager.PrintCallback() {
                    @Override public void onSuccess() {
                        uiHandler.post(() -> launchResult(true, getString(R.string.print_ok)));
                    }
                    @Override public void onError(String message) {
                        uiHandler.post(() -> launchResult(false,
                                getString(R.string.error_print) + "\n" + message));
                    }
                });
                break;

            case INVALID:
                blinkDots();
                if (r.lockoutSeconds > 0) {
                    // Backend says: lock now for this many seconds
                    applyLockout(r.lockoutSeconds);
                } else {
                    // Wrong code, not locked yet — show remaining attempts
                    String msg = getString(R.string.error_invalid);
                    if (r.attemptsRemaining > 0) {
                        msg += "\n" + getString(R.string.attempts_remaining, r.attemptsRemaining);
                    }
                    showStatus(msg, true);
                    scheduleReset(AppConfig.ERROR_RESET_DELAY_MS);
                }
                break;

            case LOCKED:
                // Backend says: already locked, here's the remaining time
                applyLockout(r.lockoutSeconds);
                break;

            case ERROR:
                showStatus(getString(R.string.error_network) + "\n" + r.message, true);
                resetPin();  // network errors: re-enable immediately, no lockout
                break;
        }
    }

    // =========================================================================
    // Lockout — driven entirely by seconds value from backend
    // =========================================================================

    /**
     * Apply a lockout of {@code seconds} seconds.
     * Disables input and starts a countdown display.
     * When the countdown reaches 0 the pad re-enables automatically.
     */
    private void applyLockout(int seconds) {
        lockedUntilMs = System.currentTimeMillis() + (long) seconds * 1000;
        setInputEnabled(false);
        pin.setLength(0);
        refreshDots();
        startCountdown();
    }

    private boolean isLocked() {
        return System.currentTimeMillis() < lockedUntilMs;
    }

    private int remainingSeconds() {
        long ms = lockedUntilMs - System.currentTimeMillis();
        return ms > 0 ? (int) Math.ceil(ms / 1000.0) : 0;
    }

    private void startCountdown() {
        stopCountdown();
        countdownRunnable = new Runnable() {
            @Override public void run() {
                if (!isLocked()) {
                    clearStatus();
                    resetPin();
                    return;
                }
                showStatus(getString(R.string.error_locked, remainingSeconds()), true);
                uiHandler.postDelayed(this, 1000);
            }
        };
        uiHandler.post(countdownRunnable);
    }

    private void stopCountdown() {
        if (countdownRunnable != null) {
            uiHandler.removeCallbacks(countdownRunnable);
            countdownRunnable = null;
        }
    }

    // =========================================================================
    // Deferred reset helpers
    // =========================================================================

    private void scheduleReset(int delayMs) {
        cancelReset();
        resetRunnable = this::resetPin;
        uiHandler.postDelayed(resetRunnable, delayMs);
    }

    private void cancelReset() {
        if (resetRunnable != null) {
            uiHandler.removeCallbacks(resetRunnable);
            resetRunnable = null;
        }
    }

    private void resetPin() {
        stopCountdown();
        cancelReset();
        pin.setLength(0);
        refreshDots();
        clearStatus();
        showLoading(false);
        if (!isLocked()) setInputEnabled(true);
    }

    // =========================================================================
    // Result screen
    // =========================================================================

    private void launchResult(boolean success, String message) {
        Intent i = new Intent(this, PrintResultActivity.class);
        i.putExtra(PrintResultActivity.EXTRA_SUCCESS, success);
        i.putExtra(PrintResultActivity.EXTRA_MESSAGE, message);
        startActivity(i);
    }

    // =========================================================================
    // UI helpers
    // =========================================================================

    private void refreshDots() {
        for (int i = 0; i < dots.length; i++) {
            dots[i].setBackgroundResource(
                    i < pin.length() ? R.drawable.dot_filled : R.drawable.dot_empty);
        }
    }

    private void setInputEnabled(boolean enabled) {
        b.gridNumpad.setAlpha(enabled ? 1f : 0.35f);
        for (int i = 0; i < b.gridNumpad.getChildCount(); i++) {
            b.gridNumpad.getChildAt(i).setEnabled(enabled);
        }
    }

    private void showLoading(boolean show) {
        b.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showStatus(String message, boolean isError) {
        b.tvStatus.setText(message);
        b.tvStatus.setTextColor(getColor(isError ? R.color.accent_red : R.color.accent_green));
    }

    private void clearStatus() { b.tvStatus.setText(""); }

    private void blinkDots() {
        b.llPinDots.animate().alpha(0.15f).setDuration(100)
                .withEndAction(() -> b.llPinDots.animate().alpha(1f).setDuration(100).start())
                .start();
    }
}
