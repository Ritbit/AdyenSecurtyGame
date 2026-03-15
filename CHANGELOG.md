# Changelog

All notable changes to this project are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

---

## [Unreleased] — Pass 5

### Fixed
- **`PrintResultActivity` was never launched** — `MainActivity` was still showing
  inline text and scheduling a reset. Now calls `launchResult()` which starts
  `PrintResultActivity` via an `Intent`. `onResume()` resets the pad cleanly
  when the user returns.
- **`Theme.CastlesApp.Fullscreen` crash** — theme was referenced in the manifest
  but never defined in `styles.xml`. Added as a child of `Theme.CastlesApp` with
  `windowFullscreen=true`.
- **Broken SVG circle paths** — `A54,54 0 1,1 59.99,6 Z` is a degenerate arc
  that renders as nothing. Replaced with an explicit cubic-bezier circle path
  that renders correctly on all API levels.
- **`PrintResultActivity` unused `Intent` import** removed (caused lint warning).
- **`tvTitle` showed `app_name` instead of `enter_code` prompt** — the prompt
  string existed but was never wired to the view. Fixed; also updated styling
  to secondary text colour to reduce visual weight.
- **`enter_code` missing from Dutch translations** — added.
- **`onResume()` added to `MainActivity`** — without it, returning from
  `PrintResultActivity` left the pad disabled and the PIN still populated.

### Added
- **`PinLockoutTest.java`** — 15 pure-JVM unit tests covering initial state,
  failure counting, attempts-remaining progression, and success reset.
  Run with `./gradlew test` — no device or emulator required.
- **`testImplementation 'junit:junit:4.13.2'`** added to `app/build.gradle`.
  `countdownRunnable`) are now tracked and cancelled in `onDestroy`.
- `executor.shutdown()` replaced with `shutdownNow()` to interrupt any in-flight
  network call when the activity is destroyed.
- `tvStatus` `minHeight` increased to `44dp` with `lineSpacingMultiplier=1.2` so
  two-line messages (invalid-code + attempts remaining, or lockout countdown +
  reason) never clip.

### Added
- `AppConfig.ERROR_RESET_DELAY_MS` — hardcoded `2000` replaced with named constant.
- `R.string.attempts_remaining` — factored out of `MainActivity` for localisation.
- `values-nl/strings.xml` — Dutch (Netherlands) full translation of all UI strings.
- `CastlesApplication.java` — `Application` subclass registered in manifest; binds
  the Castles CUP AIDL service on startup (stub + commented OPTION B ready).
- `CupServiceHolder.java` — thread-safe singleton for the AIDL binder reference.
- `scripts/deploy.sh` — shell script that builds the chosen flavor/type, checks for
  a connected adb device, installs the APK, and optionally launches the app.
- `local.properties.template` — safe template to commit; `local.properties` is gitignored.
- `.gitignore` — covers Gradle outputs, keystores, `.env`, node_modules.

### Backend (`castles-backend/server.js`)
- **Rate limiting** — 10 requests/minute per IP (in-process token bucket);
  returns HTTP 429 with JSON error when exceeded.
- **Graceful shutdown** — `SIGTERM` / `SIGINT` handlers drain in-flight requests
  before exiting; forced exit after 5 s if draining stalls.
- **Improved ticket layout** — Dutch field labels, dashed tear lines, deterministic
  barcode pattern seeded from the code, proper footer text.
- **Error handling** — try/catch around canvas render; returns HTTP 500 on failure.
- `GET /health` now includes `uptime` in seconds.

---

## [1.0.0] — initial

### Added
- PIN pad UI with 4-digit entry and dot indicators.
- `ApiClient` — POST code to backend, receive PNG/JPEG bitmap.
- `PrinterManager` stub — scales bitmap, ready for Castles SDK wiring.
- `AndroidManifest`, layout, styles, colors, strings.
