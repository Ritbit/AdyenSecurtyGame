# Castles S1F2 – Ticket Print App

An Android app for the **Castles S1F2** payment terminal that:

1. Shows a 4-digit PIN pad.
2. POSTs the code to your backend when all 4 digits are entered.
3. Receives a PNG/JPEG bitmap in the HTTP response.
4. Dithers and prints the bitmap on the built-in 58 mm thermal printer.
5. Locks out further attempts after 4 consecutive wrong codes (escalating cooldown).

---

## Quick start

### 1 — Prerequisites

| Tool | Version |
|------|---------|
| Android Studio | Hedgehog 2023.1+ |
| JDK | 17 |
| Node.js | 18+ (mock backend only) |
| Castles S1F2 SDK | from Castles developer portal |

---

### 2 — Get the Castles SDK

Register at **https://www.castlestech.com** → Developer Portal → S1F2 SDK.
Place the downloaded AAR at:

```
app/libs/castles_sdk.aar
```

Un-comment the line in `app/build.gradle`:
```groovy
implementation(name: 'castles_sdk', ext: 'aar')
```

---

### 3 — Wire the printer in `PrinterManager.java`

Un-comment **OPTION A** (direct `PrintManager`) or **OPTION B** (AIDL `CUPService`)
and delete the STUB block.  Refer to your SDK release notes for the exact class name.

---

### 4 — Configure the backend URL

Edit the IP/hostname in `app/build.gradle` under `productFlavors`:

```groovy
mock { buildConfigField "String", "BACKEND_BASE_URL", '"http://192.168.x.x:3000"' }
prod { buildConfigField "String", "BACKEND_BASE_URL", '"https://api.yourcompany.com"' }
```

All other settings (PIN length, timeouts, printer width, success delay) are in `AppConfig.java`.

---

### 5 — Start the mock backend

```bash
cd castles-backend
npm install
node server.js          # listens on :3000, all interfaces
```

Valid test codes: **1234  0000  9999  4242**

The server generates a 384 × 500 px PNG ticket on the fly and returns it as the
HTTP response body.

Connect the terminal and your PC to the same Wi-Fi.  Set the mock flavor IP in
`build.gradle` to your PC's LAN address.

---

### 6 — Build & install

```bash
# Debug build pointing at local mock server
./gradlew assembleMockDebug
adb install app/build/outputs/apk/mock/debug/app-mock-debug.apk

# Release build pointing at production
./gradlew assembleProdRelease
adb install app/build/outputs/apk/prod/release/app-prod-release.apk
```

Or open in Android Studio, select the build variant in the **Build Variants** panel,
and press ▶.

---

## Project structure

```
castles-app/
├── app/
│   ├── libs/                          ← place castles_sdk.aar here
│   └── src/main/
│       ├── java/com/example/castlesapp/
│       │   ├── AppConfig.java         single config file
│       │   ├── MainActivity.java      PIN UI + orchestration + lockout
│       │   ├── PinLockout.java        brute-force protection
│       │   ├── RetryApiClient.java    HTTP + exponential back-off
│       │   ├── ApiClient.java         (simple, no retry — kept for reference)
│       │   └── PrinterManager.java    Castles SDK wrapper + dithering
│       ├── res/
│       │   ├── layout/activity_main.xml
│       │   ├── values/{colors,strings,styles}.xml
│       │   ├── drawable/{dot_empty,dot_filled,ic_launcher_*}.xml
│       │   └── xml/network_security_config.xml
│       └── AndroidManifest.xml
├── build.gradle
├── settings.gradle
├── gradlew  /  gradlew.bat
├── CHANGELOG.md
└── README.md

castles-backend/
├── server.js     Express mock server — generates PNG tickets
└── package.json
```

---

## Backend contract

```
POST  /api/validate
Content-Type: application/json
{ "code": "1234" }

200  → raw PNG or JPEG bytes          (printed)
401/403/404 → invalid code JSON       (user sees error; lockout counter incremented)
5xx  → server error                   (retried up to 3× with back-off)
```

---

## Brute-force lockout schedule

| Consecutive failures | Lockout duration |
|---------------------|-----------------|
| 1 – 4               | none (warning shown on last) |
| 5                   | 30 seconds |
| 6                   | 60 seconds |
| 7                   | 120 seconds |
| 8+                  | 300 seconds (5 min, capped) |

The counter resets on a successful print.
Pair this with server-side rate-limiting for defence-in-depth.

---

## Notes

- The S1F2 thermal head is **58 mm = 384 px at 203 dpi**.  
  `PrinterManager` auto-scales and Floyd-Steinberg dithers before printing.
- Targets **API 29** (Android 10), the OS version shipped on the S1F2.
- Network I/O runs on a dedicated `ExecutorService` thread; all UI updates are
  posted back to the main thread via `Handler`.
