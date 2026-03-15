package com.example.castlesapp;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

/**
 * Application subclass.
 *
 * Responsibilities:
 *  1. Bind the Castles CUP AIDL printer service on startup (OPTION B only).
 *  2. Unbind cleanly on termination.
 *
 * ── Activation ───────────────────────────────────────────────────────────────
 * This class is already registered in AndroidManifest.xml.
 * No further action is needed for OPTION A (direct PrintManager).
 *
 * For OPTION B (AIDL CUPService):
 *  1. Un-comment the ICUPService import below once the SDK AAR is in app/libs/.
 *  2. Un-comment the bindCupService() body.
 *  3. Un-comment CupServiceHolder.set(stub) in the ServiceConnection.
 *  4. Switch PrinterManager to OPTION B.
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class CastlesApplication extends Application {

    private static final String TAG = "CastlesApplication";

    // Un-comment after SDK is added:
    // import com.castles.device.ICUPService;

    // CUP service component name — verify against your SDK release notes
    private static final String CUP_PACKAGE = "com.castles.device";
    private static final String CUP_CLASS   = "com.castles.device.CUPService";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Application starting");
        bindCupService();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        // ServiceConnection is automatically unbound when the process dies,
        // but unbinding explicitly is good practice during development.
    }

    // ── CUP service binding ──────────────────────────────────────────────────

    private void bindCupService() {
        // ── STUB — remove and un-comment the block below once SDK is present ──
        Log.d(TAG, "CUP service binding skipped (SDK stub mode)");
        // ──────────────────────────────────────────────────────────────────────

        /*
        Intent intent = new Intent();
        intent.setClassName(CUP_PACKAGE, CUP_CLASS);
        boolean bound = bindService(intent, cupConnection, Context.BIND_AUTO_CREATE);
        if (!bound) {
            Log.e(TAG, "Failed to bind CUP service — printing via AIDL will not work");
        }
        */
    }

    @SuppressWarnings("unused")
    private final ServiceConnection cupConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Log.i(TAG, "CUP service connected: " + name);
            // Un-comment after SDK is added:
            // ICUPService stub = ICUPService.Stub.asInterface(binder);
            // CupServiceHolder.set(stub);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.w(TAG, "CUP service disconnected: " + name);
            // CupServiceHolder.set(null);
        }
    };
}
