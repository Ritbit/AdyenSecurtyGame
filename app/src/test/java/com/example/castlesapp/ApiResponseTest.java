package com.example.castlesapp;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link ApiResponse} factory methods and field values.
 * Pure JVM — no Android framework needed.  Run with ./gradlew test
 */
public class ApiResponseTest {

    @Test
    public void ok_hasCorrectStatus() {
        ApiResponse r = ApiResponse.ok(null);
        assertEquals(ApiResponse.Status.OK, r.status);
    }

    @Test
    public void ok_hasZeroLockoutAndAttempts() {
        ApiResponse r = ApiResponse.ok(null);
        assertEquals(0, r.lockoutSeconds);
        assertEquals(0, r.attemptsRemaining);
        assertNull(r.message);
    }

    @Test
    public void invalid_storesAttemptsAndLockout() {
        ApiResponse r = ApiResponse.invalid(3, 0);
        assertEquals(ApiResponse.Status.INVALID, r.status);
        assertEquals(3, r.attemptsRemaining);
        assertEquals(0, r.lockoutSeconds);
        assertNull(r.image);
    }

    @Test
    public void invalid_withLockout_storesSeconds() {
        ApiResponse r = ApiResponse.invalid(0, 30);
        assertEquals(ApiResponse.Status.INVALID, r.status);
        assertEquals(0,  r.attemptsRemaining);
        assertEquals(30, r.lockoutSeconds);
    }

    @Test
    public void locked_storesSeconds() {
        ApiResponse r = ApiResponse.locked(120);
        assertEquals(ApiResponse.Status.LOCKED, r.status);
        assertEquals(120, r.lockoutSeconds);
        assertEquals(0,   r.attemptsRemaining);
        assertNull(r.image);
    }

    @Test
    public void error_storesMessage() {
        ApiResponse r = ApiResponse.error("Network timeout");
        assertEquals(ApiResponse.Status.ERROR, r.status);
        assertEquals("Network timeout", r.message);
        assertNull(r.image);
        assertEquals(0, r.lockoutSeconds);
    }

    @Test
    public void locked_zeroSeconds_isZero() {
        ApiResponse r = ApiResponse.locked(0);
        assertEquals(0, r.lockoutSeconds);
    }

    @Test
    public void invalid_zeroAttemptsZeroLockout() {
        // Edge: backend may send 0/0 for final warning before locking
        ApiResponse r = ApiResponse.invalid(0, 0);
        assertEquals(ApiResponse.Status.INVALID, r.status);
        assertEquals(0, r.attemptsRemaining);
        assertEquals(0, r.lockoutSeconds);
    }
}
