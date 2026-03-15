package com.example.castlesapp;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

/**
 * Wrapper around the Castles S1F2 built-in thermal printer.
 *
 * ── SDK integration ──────────────────────────────────────────────────────────
 * 1. Download the S1F2 Android SDK from https://www.castlestech.com
 * 2. Place the .aar in  app/libs/castles_sdk.aar
 * 3. Un-comment the dependency in app/build.gradle
 * 4. Un-comment ONE of the SDK blocks (OPTION A or B) in printBitmap() below
 * 5. Delete the "STUB" block
 *
 * ── Castles printer result codes (S1F2 SDK) ──────────────────────────────────
 *   0   SUCCESS
 *  -1   PRINTER_BUSY
 *  -2   OUT_OF_PAPER
 *  -3   FORMAT_ERROR
 *  -4   UNKNOWN_ERROR
 *  -6   OVERHEAT
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class PrinterManager {

    private static final String TAG = "PrinterManager";

    // SDK imports — un-comment ONE after the AAR is in app/libs/
    // import com.castles.castles.printer.PrintManager;   // older SDK
    // import com.castles.device.printer.PrintManager;    // newer SDK

    // Castles result-code constants (mirrored here so code compiles without the AAR)
    private static final int RESULT_OK          =  0;
    private static final int RESULT_BUSY        = -1;
    private static final int RESULT_OUT_OF_PAPER= -2;
    private static final int RESULT_FORMAT_ERR  = -3;
    private static final int RESULT_OVERHEAT    = -6;

    private static final int PRINTER_WIDTH_PX   = AppConfig.PRINTER_WIDTH_PX;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public interface PrintCallback {
        void onSuccess();
        /** @param reason  Human-readable description of what went wrong. */
        void onError(String reason);
    }

    /**
     * Print a bitmap on the built-in S1F2 thermal printer.
     * Must be called from a background thread.
     *
     * @param src      Bitmap from the backend. Auto-scaled to PRINTER_WIDTH_PX.
     * @param callback Invoked on the calling thread when done.
     */
    public static void printBitmap(Bitmap src, PrintCallback callback) {
        try {
            Bitmap bmp = scaleBitmap(src);
            bmp = toMonochrome(bmp);      // thermal printers render best in 1-bit

            // =================================================================
            // OPTION A — Direct PrintManager  (most S1F2 SDK versions)
            // =================================================================
            /*
            PrintManager pm = PrintManager.getInstance();
            int init = pm.init();
            if (init != RESULT_OK) {
                callback.onError(printerError(init));
                return;
            }
            int result = pm.printBitmap(bmp);
            if (result == RESULT_OK) {
                callback.onSuccess();
            } else {
                callback.onError(printerError(result));
            }
            */

            // =================================================================
            // OPTION B — CUPService AIDL  (newer firmware)
            // Obtain ICUPService via bindService() in your Application class,
            // then pass it in (or use a singleton accessor).
            // =================================================================
            /*
            ICUPService cup = CupServiceHolder.get();
            if (cup == null) { callback.onError("Printer service not bound"); return; }
            int result = cup.printBitmap(bitmapToByteArray(bmp));
            if (result == RESULT_OK) callback.onSuccess();
            else                     callback.onError(printerError(result));
            */

            // =================================================================
            // STUB — remove once the SDK is integrated
            // =================================================================
            Log.w(TAG, "STUB: would print " + bmp.getWidth() + "×" + bmp.getHeight() + " px");
            callback.onSuccess();
            // =================================================================

        } catch (Exception e) {
            Log.e(TAG, "printBitmap exception", e);
            callback.onError("Exception: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /** Scale bitmap to the printer head width, preserving aspect ratio. */
    private static Bitmap scaleBitmap(Bitmap src) {
        if (src.getWidth() == PRINTER_WIDTH_PX) return src;
        int h = (int) ((float) src.getHeight() / src.getWidth() * PRINTER_WIDTH_PX);
        return Bitmap.createScaledBitmap(src, PRINTER_WIDTH_PX, h, true);
    }

    /**
     * Convert to greyscale then apply Floyd-Steinberg dithering to produce a
     * 1-bit-like image that thermal printers render cleanly.
     *
     * For full colour bitmaps just returning the scaled bitmap also works, but
     * dithering gives much crisper results on a 203-dpi thermal head.
     */
    private static Bitmap toMonochrome(Bitmap src) {
        int w = src.getWidth(), h = src.getHeight();
        int[] pixels = new int[w * h];
        src.getPixels(pixels, 0, w, 0, 0, w, h);

        // Convert each pixel to greyscale luminance (0-255)
        float[] grey = new float[w * h];
        for (int i = 0; i < pixels.length; i++) {
            int c = pixels[i];
            grey[i] = 0.299f * Color.red(c) + 0.587f * Color.green(c) + 0.114f * Color.blue(c);
        }

        // Floyd-Steinberg dithering
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int idx   = y * w + x;
                float old = grey[idx];
                float nw  = old < 128f ? 0f : 255f;
                grey[idx] = nw;
                float err = old - nw;
                if (x + 1 < w)           grey[idx + 1]     += err * 7f / 16f;
                if (y + 1 < h) {
                    if (x > 0)           grey[idx + w - 1] += err * 3f / 16f;
                                         grey[idx + w]     += err * 5f / 16f;
                    if (x + 1 < w)       grey[idx + w + 1] += err * 1f / 16f;
                }
            }
        }

        // Write back as ARGB_8888
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        for (int i = 0; i < pixels.length; i++) {
            int g = (int) Math.max(0, Math.min(255, grey[i]));
            pixels[i] = Color.rgb(g, g, g);
        }
        out.setPixels(pixels, 0, w, 0, 0, w, h);
        return out;
    }

    /** Translate SDK result code to a user-readable message. */
    private static String printerError(int code) {
        switch (code) {
            case RESULT_BUSY:         return "Printer is busy — please wait.";
            case RESULT_OUT_OF_PAPER: return "Out of paper — please reload.";
            case RESULT_FORMAT_ERR:   return "Image format error.";
            case RESULT_OVERHEAT:     return "Printer overheated — please wait.";
            default:                  return "Printer error (code " + code + ").";
        }
    }

    /** Convert Bitmap to PNG byte array (used by OPTION B / AIDL). */
    @SuppressWarnings("unused")
    private static byte[] bitmapToByteArray(Bitmap bmp) {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
        return out.toByteArray();
    }
}
