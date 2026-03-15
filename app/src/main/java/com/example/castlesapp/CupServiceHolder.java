package com.example.castlesapp;

/**
 * Singleton holder for the Castles CUP service AIDL stub.
 *
 * Used by PrinterManager OPTION B only.
 *
 * CastlesApplication binds the service on startup and stores the binder here.
 * PrinterManager reads it on every print call.
 *
 * ── How to activate ──────────────────────────────────────────────────────────
 * 1. Un-comment the import in CastlesApplication.java once the SDK AAR is added.
 * 2. Un-comment OPTION B in PrinterManager.java.
 * 3. Delete the STUB block in PrinterManager.java.
 * ─────────────────────────────────────────────────────────────────────────────
 */
public final class CupServiceHolder {

    private CupServiceHolder() {}

    // Un-comment after SDK is added:
    // private static volatile ICUPService instance;
    // public static ICUPService get()               { return instance; }
    // public static void       set(ICUPService svc) { instance = svc;  }

    /** Stub — always returns null until the SDK is integrated. */
    public static Object get() { return null; }
}
