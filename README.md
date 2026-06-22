# TimeToPay - NFC WearOS Automater

A tiny app that automatically turns NFC off and on.

TimeToPay is a standalone Wear OS app. Pick which apps should enable NFC, for example Google Wallet or Samsung Wallet, and NFC turns on only while those apps are open. No phone or sign-up required.

This app must be sideloaded with a one-time ADB setup on a PC. It can't be in the Play Store because of the permissions required.

## Features

- Auto NFC: NFC on while the selected app is open; off when you leave it.
- You choose the apps: Use something else instead of Google Wallet? All good! Choose any launchable app on your watch. 
- Standalone watch app: Works without a phone. No sync, no cloud, no network.

## Requirements

| | |
|---|---|
| Watch app | Wear OS 3+ with NFC |
| Setup | PC with [ADB](https://developer.android.com/tools/releases/platform-tools); watch and PC on same Wi-Fi for wireless debugging |

## Setup

Download the latest APK from [GitHub Releases](https://github.com/kattcrazy/TimeToPay/releases) (`timetopay-wear.apk`).

### 1. Prepare the watch

1. Settings → About watch → Software → tap Software version 5 times (Developer mode).
2. Settings → Developer options → ADB debugging ON, Wireless debugging ON.
3. Watch and PC on the same Wi-Fi.

Galaxy Watch 6/7: If pairing fails, temporarily turn off Bluetooth so Wi-Fi stays active.

### 2. Pair and connect

On the watch: Developer options → Wireless debugging → Pair new device.

```bash
adb pair WATCH_IP:PAIRING_PORT
adb connect WATCH_IP:CONNECTION_PORT
adb devices
```

### 3. Install and grant permissions

```bash
adb install timetopay-wear.apk
adb shell pm grant com.timetopay android.permission.WRITE_SECURE_SETTINGS
```

Re-run `pm grant` after uninstall or reinstall.

### 4. Enable accessibility

```bash
adb shell settings get secure enabled_accessibility_services
```

If empty:

```bash
adb shell settings put secure enabled_accessibility_services com.timetopay/.TimeToPayAccessibilityService
adb shell settings put secure accessibility_enabled 1
```

If other services are already enabled, append with a colon (do not replace them):

```bash
adb shell settings put secure enabled_accessibility_services EXISTING:com.timetopay/.TimeToPayAccessibilityService
adb shell settings put secure accessibility_enabled 1
```

### 5. Choose apps on the watch

Open TimeToPay → Choose apps → tick your payment app(s) → Save.

## Releasing

1. Bump `versionCode` and `versionName` in `wear/build.gradle.kts`.
2. Commit and push to GitHub.
3. Create a [GitHub Release](https://github.com/kattcrazy/TimeToPay/releases/new) with a tag (for example `v1.0.0`) and publish it.

GitHub Actions builds the minified APK and attaches `timetopay-wear.apk` automatically when the release is published. You can also run the workflow manually from the Actions tab.

## Uninstall

```bash
adb uninstall com.timetopay
```
or use the uninstall button in your watch's app list.

## Privacy

No network permissions needed- no data leaves your watch. The app simply compares foreground app package names to your local selection stored in SharedPreferences.

## License

This project uses the [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0.html). See [LICENSE](LICENSE) for the full legal text. In short: you can use, change, and share it freely. If you distribute a modified version, you must offer it under the same license and share the source too, so the work (and its derivatives) stay open. You cannot take this code, tweak it, and ship it as a closed product.

## About

Leaving NFC on causes security risks (accidential payments), but you still want to pay quickly? This app was built to solve that problem! Please [open an issue](https://github.com/kattcrazy/TimeToPay/issues) if it  isn't working or you have an idea.

If TimeToPay helps speed up your day-to-day payments, consider supporting me [here](https://kattcrazy.nz/product/support-me/) :)
