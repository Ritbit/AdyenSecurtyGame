# ── Castles SDK ───────────────────────────────────────────────────────────────
-keep class com.castles.** { *; }
-dontwarn com.castles.**

# ── OkHttp ────────────────────────────────────────────────────────────────────
# JSR 305 annotations
-dontwarn javax.annotation.**

# OkHttp internals use reflection — keep the public API and key internals
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-dontwarn org.codehaus.mojo.animal_sniffer.*
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Keep request/response bodies (used reflectively by OkHttp)
-keep class okhttp3.RequestBody { *; }
-keep class okhttp3.ResponseBody { *; }
-keep class okhttp3.MediaType { *; }

# ── App classes that must survive shrinking ───────────────────────────────────
# Callback interfaces are referenced by anonymous inner classes — keep them
-keep interface com.example.castlesapp.RetryApiClient$ApiCallback { *; }
-keep interface com.example.castlesapp.PrinterManager$PrintCallback { *; }
