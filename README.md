# <img src="app_mark.png" alt="TimeToPay icon" width="36" /> TimeToPay - NFC WearOS Automater <img src="app_mark.png" alt="TimeToPay icon" width="36" />

A tiny app that automatically turns NFC off and on.

TimeToPay is a standalone Wear OS app. Pick which apps should enable NFC, for example Google Wallet or Samsung Wallet, and NFC turns on only while those apps are open. No phone or sign-up required.

This app must be sideloaded. Accessibility can be enabled on the watch; one ADB command is still needed for NFC control. It can't be in the Play Store because of the permissions required.

## Features

- Auto NFC: NFC on while selected apps are open; off when you leave them.
- You choose the apps: Use something else instead of Google Wallet? All good! Choose any launchable app on your watch. 
- Standalone watch app: Works without a phone. No sync, no cloud, no network.

## Requirements

| | |
|---|---|
| Watch app | Wear OS 3+ with NFC |
| Setup | Watch for accessibility; PC with [ADB](https://developer.android.com/tools/releases/platform-tools) for one NFC permission grant |

## Setup

Download the latest APK from [GitHub Releases](https://github.com/kattcrazy/TimeToPay/releases) (`timetopay-wear.apk`) and install it on your watch.

### 1. Enable accessibility on the watch

1. Open TimeToPay on your watch.
2. Tap Open accessibility and turn on TimeToPay.
3. If Android blocks it, tap Open app info, open the menu (⋮), choose Allow restricted settings, then try accessibility again.

### 2. Grant NFC control (one-time ADB)

NFC toggling still needs a one-time PC grant. Enable wireless debugging on your watch (Settings → Developer options), pair with ADB, then run:

```bash
adb shell pm grant com.timetopay android.permission.WRITE_SECURE_SETTINGS
```

Re-run after uninstall or reinstall.

Galaxy Watch 6/7: If ADB pairing fails, temporarily turn off Bluetooth so Wi-Fi stays active.

### 3. Choose apps on the watch

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

Leaving NFC on causes security risks (accidential payments). But... you still want to pay quickly, of course! This app was built to solve that problem! Please [open an issue](https://github.com/kattcrazy/TimeToPay/issues) if it  isn't working or you have an idea.

If TimeToPay helps speed up your day-to-day payments, consider supporting me [here](https://kattcrazy.nz/product/support-me/) :)
