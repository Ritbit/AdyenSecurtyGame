#!/usr/bin/env bash
# deploy.sh — Build the chosen flavor and push to a connected Castles S1F2 via adb
#
# Usage:
#   ./scripts/deploy.sh mock debug      # → assembleMockDebug
#   ./scripts/deploy.sh prod release    # → assembleProdRelease (needs keystore)
#
# Prerequisites:
#   - Android SDK installed, $ANDROID_HOME set
#   - adb in PATH
#   - For release builds: set KEYSTORE_PATH, KEYSTORE_PASS, KEY_ALIAS, KEY_PASS
#     in the environment or in a .env file (never commit .env)

set -euo pipefail

FLAVOR=${1:-mock}
BUILD_TYPE=${2:-debug}
VARIANT="${FLAVOR}${BUILD_TYPE^}"     # e.g. mockDebug, prodRelease

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
APK_DIR="$PROJECT_DIR/app/build/outputs/apk/$FLAVOR/$BUILD_TYPE"
APP_PACKAGE="com.example.castlesapp"

# Load optional .env (for keystore secrets on dev machines)
if [[ -f "$PROJECT_DIR/.env" ]]; then
    # shellcheck disable=SC1091
    set -a; source "$PROJECT_DIR/.env"; set +a
fi

echo "========================================"
echo "  Castles S1F2 Deploy Script"
echo "  Flavor     : $FLAVOR"
echo "  Build type : $BUILD_TYPE"
echo "  Variant    : $VARIANT"
echo "========================================"

# ── 1. Build ──────────────────────────────────────────────────────────────────
echo ""
echo "[1/3] Building assemble${VARIANT^}…"
cd "$PROJECT_DIR"
./gradlew "assemble${VARIANT^}" --quiet

# Find the APK (there should be exactly one)
APK=$(find "$APK_DIR" -name "*.apk" | head -1)
if [[ -z "$APK" ]]; then
    echo "ERROR: No APK found in $APK_DIR"
    exit 1
fi
echo "      APK: $APK"

# ── 2. Check adb device ───────────────────────────────────────────────────────
echo ""
echo "[2/3] Checking for connected device…"
DEVICES=$(adb devices | grep -v "^List" | grep -v "^$" | awk '{print $1}')
if [[ -z "$DEVICES" ]]; then
    echo "ERROR: No adb device connected."
    echo "       Connect the S1F2 via USB and enable USB debugging in developer options."
    exit 1
fi
DEVICE_COUNT=$(echo "$DEVICES" | wc -l | tr -d ' ')
echo "      Found $DEVICE_COUNT device(s): $DEVICES"

# ── 3. Install ────────────────────────────────────────────────────────────────
echo ""
echo "[3/3] Installing APK…"
adb install -r "$APK"

echo ""
echo "✓ Installed successfully."
echo "  Package : $APP_PACKAGE"
echo ""

# Optional: launch the app immediately
read -rp "Launch app now? [y/N] " launch
if [[ "$launch" =~ ^[Yy]$ ]]; then
    if [[ "$BUILD_TYPE" == "debug" ]]; then
        LAUNCH_PKG="${APP_PACKAGE}.debug"
    else
        LAUNCH_PKG="${APP_PACKAGE}"
    fi
    adb shell am start -n "${LAUNCH_PKG}/${APP_PACKAGE}.MainActivity"
    echo "  App launched."
fi
