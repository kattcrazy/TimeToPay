# Installation

Download the latest APK from [GitHub Releases](https://github.com/kattcrazy/TimeToPay/releases) (`timetopay-wear.apk`).

## Requirements

Watch with Wear OS 3+ with and NFC
PC with [ADB](https://developer.android.com/tools/releases/platform-tools) for adb sideload and permissions grant.

Watch support varies, see [COMPATIBILITY.md](COMPATIBILITY.md).

<details>
<summary>Connect via ADB</summary>

You need [ADB platform-tools](https://developer.android.com/tools/releases/platform-tools) on your PC and a connection to your watch.

1. Enable developer options on the watch. Settings -> About watch -> Tap serial number 5+ times
2. Now, still on the watch... Settings -> Developer options -> enable Wireless debugging -> Pair new device
3. Pair from your PC (use the IP and ports shown on the watch):
   ```bash
   adb pair WATCH_IP:PAIRING_PORT
   adb connect WATCH_IP:CONNECTION_PORT
   ```
   The password will be shown on the watch. Type it in.
4. Confirm the watch appears when you run `adb devices`

Note for Galaxy Watch 6/7: If pairing fails, temporarily turn off Bluetooth so Wi‑Fi debugging stays active.
</details>

## 1. Install the APK

Sideload with ADB:

```bash
adb install -r timetopay-wear.apk
```

## 2. Enable accessibility on the watch

1. Open TimeToPay on your watch.
2. Tap Open accessibility -> Install apps and toggle on TimeToPay.
3. If Android blocks it, tap Open app info, open the menu (⋮), choose Allow restricted settings, then try accessibility again.

## 3. Grant NFC control (one-time ADB)

NFC toggling needs a high-level permission.

```bash
adb shell pm grant com.timetopay android.permission.WRITE_SECURE_SETTINGS
```

Re-run after uninstall or reinstall.

Galaxy Watch 6/7: If ADB pairing fails, temporarily turn off Bluetooth so Wi‑Fi stays active.

## 4. Choose apps on the watch

Open TimeToPay -> Choose apps -> select your payment app(s) -> Save.

## Finished!