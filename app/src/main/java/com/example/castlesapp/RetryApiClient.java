package com.example.castlesapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Sends a code to the backend and returns a fully-parsed {@link ApiResponse}.
 *
 * Protocol
 * ────────
 * POST  AppConfig.BASE_URL
 * Body: { "code": "1234", "terminalId": "terminal-01" }
 *
 * All server responses are JSON:
 *   { "status": "ok",      "image": "<base64 PNG>" }
 *   { "status": "invalid", "attemptsRemaining": 2, "lockoutSeconds": 0 }
 *   { "status": "locked",  "lockoutSeconds": 47 }
 *   { "status": "error",   "message": "..." }
 *
 * Retry policy
 * ────────────
 * - Retries up to MAX_RETRIES times for network errors and HTTP 5xx.
 * - Any well-formed JSON response (ok / invalid / locked) is never retried.
 * - InterruptedException propagates cleanly for executor.shutdownNow().
 */
public class RetryApiClient {

    private static final String TAG           = "RetryApiClient";
    private static final int    MAX_RETRIES   = 3;
    private static final long   BASE_DELAY_MS = 500L;

    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(AppConfig.CONNECT_TIMEOUT_S, TimeUnit.SECONDS)
            .readTimeout(AppConfig.READ_TIMEOUT_S,       TimeUnit.SECONDS)
            .writeTimeout(AppConfig.CONNECT_TIMEOUT_S,   TimeUnit.SECONDS)
            .build();

    public interface Callback {
        void onResult(ApiResponse response);
    }

    /**
     * Validate a code. Blocking — call from a background thread.
     * Result is always delivered via {@code callback}, never throws.
     */
    public static void validateCode(String code, Callback callback) {
        String json;
        try {
            JSONObject body = new JSONObject();
            body.put("code",       code);
            body.put("terminalId", AppConfig.TERMINAL_ID);
            json = body.toString();
        } catch (Exception e) {
            callback.onResult(ApiResponse.error("JSON build error: " + e.getMessage()));
            return;
        }

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {

            if (Thread.currentThread().isInterrupted()) {
                callback.onResult(ApiResponse.error("Cancelled"));
                return;
            }

            RequestBody reqBody = RequestBody.create(
                    json, MediaType.get("application/json; charset=utf-8"));
            Request req = new Request.Builder()
                    .url(AppConfig.BASE_URL)
                    .post(reqBody)
                    .build();

            try (Response response = HTTP.newCall(req).execute()) {
                int http = response.code();
                Log.d(TAG, "Attempt " + attempt + " → HTTP " + http);

                String bodyStr = response.body() != null ? response.body().string() : "";

                // ── Parse JSON response ───────────────────────────────────────
                ApiResponse parsed = parseResponse(http, bodyStr);
                if (parsed != null) {
                    // A well-formed response — return it regardless of status
                    callback.onResult(parsed);
                    return;
                }

                // ── Unparseable / server error — retry ───────────────────────
                Log.w(TAG, "Unparseable response on attempt " + attempt
                        + " (HTTP " + http + "): " + bodyStr);

                if (attempt == MAX_RETRIES) {
                    callback.onResult(ApiResponse.error(
                            "Server error (HTTP " + http + ") after " + MAX_RETRIES + " attempts"));
                    return;
                }

            } catch (IOException e) {
                Log.w(TAG, "IO error on attempt " + attempt + ": " + e.getMessage());
                if (attempt == MAX_RETRIES) {
                    callback.onResult(ApiResponse.error("Network error: " + e.getMessage()));
                    return;
                }
            }

            // Back-off before next attempt
            try {
                Thread.sleep(BASE_DELAY_MS * (1L << (attempt - 1)));
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                callback.onResult(ApiResponse.error("Cancelled"));
                return;
            }
        }
    }

    // ── Response parser ───────────────────────────────────────────────────────

    /**
     * Parse a JSON response body into an {@link ApiResponse}.
     * Returns null if the body is not valid JSON or has no recognised "status".
     */
    private static ApiResponse parseResponse(int httpCode, String body) {
        if (body == null || body.isEmpty()) return null;
        try {
            JSONObject j      = new JSONObject(body);
            String     status = j.optString("status", "");

            switch (status) {
                case "ok": {
                    String b64 = j.optString("image", "");
                    if (b64.isEmpty()) return null;
                    byte[] bytes = Base64.decode(b64, Base64.DEFAULT);
                    Bitmap bmp   = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    if (bmp == null) return null;
                    return ApiResponse.ok(bmp);
                }
                case "invalid": {
                    int attemptsRemaining = j.optInt("attemptsRemaining", 0);
                    int lockoutSeconds    = j.optInt("lockoutSeconds",    0);
                    return ApiResponse.invalid(attemptsRemaining, lockoutSeconds);
                }
                case "locked": {
                    int lockoutSeconds = j.optInt("lockoutSeconds", 0);
                    return ApiResponse.locked(lockoutSeconds);
                }
                case "error": {
                    return ApiResponse.error(j.optString("message", "Unknown server error"));
                }
                default:
                    // Unrecognised status — treat as transient, allow retry
                    return null;
            }
        } catch (Exception e) {
            Log.w(TAG, "JSON parse error: " + e.getMessage());
            return null;
        }
    }
}
