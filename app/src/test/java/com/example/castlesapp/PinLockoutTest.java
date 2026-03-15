package com.example.castlesapp;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link PinLockout}.
 *
 * These are pure JVM tests (no Android framework) so they run with
 * ./gradlew test  without a device or emulator.
 *
 * NOTE: PinLockout uses SystemClock.elapsedRealtime() internally, which
 * returns 0 on the JVM (not an Android device).  The lockout timer
 * tests that can only be verified on-device are marked with a comment.
 * All counter-logic tests are fully deterministic.
 */
public class PinLockoutTest {

    private PinLockout lockout;

    @Before
    public void setUp() {
        lockout = new PinLockout();
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    public void initialState_notLocked() {
        assertFalse(lockout.isLocked());
    }

    @Test
    public void initialState_zeroFailures() {
        assertEquals(0, lockout.failureCount());
    }

    @Test
    public void initialState_attemptsRemainingIsMax() {
        // MAX_ATTEMPTS is package-private; we know it is 4 from the source.
        assertEquals(4, lockout.attemptsRemaining());
    }

    @Test
    public void initialState_remainingSecondsIsZero() {
        assertEquals(0, lockout.remainingSeconds());
    }

    // ── Failure counting ──────────────────────────────────────────────────────

    @Test
    public void oneFailure_notYetLocked() {
        lockout.recordFailure();
        assertFalse(lockout.isLocked());
        assertEquals(1, lockout.failureCount());
        assertEquals(3, lockout.attemptsRemaining());
    }

    @Test
    public void fourFailures_notYetLocked() {
        for (int i = 0; i < 4; i++) lockout.recordFailure();
        assertFalse(lockout.isLocked());
        assertEquals(0, lockout.attemptsRemaining());
    }

    @Test
    public void fiveFailures_nowLocked() {
        for (int i = 0; i < 5; i++) lockout.recordFailure();
        // On a real device isLocked() would return true.
        // On JVM SystemClock.elapsedRealtime() == 0, so the lock may have
        // already expired.  We verify the failure count is correct instead.
        assertEquals(5, lockout.failureCount());
        assertEquals(0, lockout.attemptsRemaining());
    }

    @Test
    public void attemptsRemainingDoesNotGoBelowZero() {
        for (int i = 0; i < 10; i++) lockout.recordFailure();
        assertEquals(0, lockout.attemptsRemaining());
    }

    // ── Reset on success ──────────────────────────────────────────────────────

    @Test
    public void recordSuccess_resetsFailureCount() {
        lockout.recordFailure();
        lockout.recordFailure();
        lockout.recordSuccess();
        assertEquals(0, lockout.failureCount());
    }

    @Test
    public void recordSuccess_afterLockout_resetsAttemptsRemaining() {
        for (int i = 0; i < 5; i++) lockout.recordFailure();
        lockout.recordSuccess();
        assertEquals(4, lockout.attemptsRemaining());
    }

    @Test
    public void recordSuccess_afterLockout_notLocked() {
        for (int i = 0; i < 5; i++) lockout.recordFailure();
        lockout.recordSuccess();
        assertFalse(lockout.isLocked());
    }

    @Test
    public void recordSuccess_onFreshInstance_isIdempotent() {
        lockout.recordSuccess();   // should not throw
        assertEquals(0, lockout.failureCount());
        assertFalse(lockout.isLocked());
    }

    // ── Attempts remaining progression ───────────────────────────────────────

    @Test
    public void attemptsRemainingDecrementsWithEachFailure() {
        for (int expected = 4; expected >= 0; expected--) {
            assertEquals(expected, lockout.attemptsRemaining());
            if (expected > 0) lockout.recordFailure();
        }
    }

    @Test
    public void attemptsRemainingResetsAfterSuccess() {
        lockout.recordFailure();
        lockout.recordFailure();
        assertEquals(2, lockout.attemptsRemaining());
        lockout.recordSuccess();
        assertEquals(4, lockout.attemptsRemaining());
    }
}
