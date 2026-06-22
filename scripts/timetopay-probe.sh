# TimeToPay device probe - run with watch connected via ADB.
# Usage: bash scripts/timetopay-probe.sh
# Copy the full output into a device report issue.

set -euo pipefail

section() {
  echo ""
  echo "=== $1 ==="
}

adb_cmd() {
  adb "$@"
}

get_mstate() {
  adb_cmd shell dumpsys nfc 2>/dev/null | grep "^mState=" | head -1 || echo "mState=unknown"
}

section "TIMETOPAY PROBE REPORT"
echo "Generated: $(date -u +"%Y-%m-%dT%H:%M:%SZ")"

section "DEVICE"
adb_cmd shell getprop ro.product.manufacturer
adb_cmd shell getprop ro.product.model
adb_cmd shell getprop ro.product.device
adb_cmd shell getprop ro.build.version.release
adb_cmd shell getprop ro.build.version.sdk
adb_cmd shell getprop ro.build.display.id

section "NFC BASELINE"
echo "settings.global.nfc_on=$(adb_cmd shell settings get global nfc_on)"
get_mstate
adb_cmd shell dumpsys nfc 2>/dev/null | grep -E "^mAlwaysOnState=|^mScreenState=" | head -5 || true

section "NFC METHOD TEST (settings.global.nfc_on)"
ORIG_NFC_ON="$(adb_cmd shell settings get global nfc_on | tr -d '\r')"
ORIG_MSTATE="$(get_mstate)"
echo "Original: nfc_on=$ORIG_NFC_ON, $ORIG_MSTATE"

echo "--- Enable via settings (nfc_on=1) ---"
adb_cmd shell settings put global nfc_on 1
sleep 2
echo "After enable: nfc_on=$(adb_cmd shell settings get global nfc_on | tr -d '\r'), $(get_mstate)"

echo "--- Disable via settings (nfc_on=0) ---"
adb_cmd shell settings put global nfc_on 0
sleep 2
echo "After disable: nfc_on=$(adb_cmd shell settings get global nfc_on | tr -d '\r'), $(get_mstate)"

echo "--- Restore original ---"
if [ "$ORIG_NFC_ON" = "null" ] || [ -z "$ORIG_NFC_ON" ]; then
  adb_cmd shell settings delete global nfc_on 2>/dev/null || true
else
  adb_cmd shell settings put global nfc_on "$ORIG_NFC_ON"
fi
sleep 1
echo "Restored: nfc_on=$(adb_cmd shell settings get global nfc_on | tr -d '\r'), $(get_mstate)"

section "SYSTEM UI PACKAGES"
adb_cmd shell pm list packages | grep -i systemui || echo "(none found)"

section "QUICK PANEL FOCUS"
echo "Open quick settings on your watch now, then press Enter..."
read -r
adb_cmd shell dumpsys window 2>/dev/null | grep -E "mCurrentFocus=|mFocusedApp=|StatusBar" | head -10 || true

section "TIMETOPAY SETUP"
if adb_cmd shell pm list packages com.timetopay 2>/dev/null | grep -q timetopay; then
  echo "installed=yes"
  echo "versionName=$(adb_cmd shell dumpsys package com.timetopay 2>/dev/null | grep versionName | head -1 | tr -d '\r' || echo unknown)"
  echo "accessibility=$(adb_cmd shell settings get secure enabled_accessibility_services 2>/dev/null | tr -d '\r' | grep -o 'com.timetopay[^:]*' || echo not enabled)"
  echo "WRITE_SECURE_SETTINGS=$(adb_cmd shell dumpsys package com.timetopay 2>/dev/null | grep -A1 WRITE_SECURE_SETTINGS | grep granted | head -1 | tr -d '\r' || echo unknown)"
  echo "Recent TimeToPay NFC logs:"
  adb_cmd logcat -d -s TimeToPay:* 2>/dev/null | tail -15 || echo "(none)"
else
  echo "installed=no"
  echo "Install TimeToPay and re-run for runtime NFC logs."
fi

section "END OF REPORT"
echo "Paste everything above into: https://github.com/kattcrazy/TimeToPay/issues/new?template=device-report.yml"
