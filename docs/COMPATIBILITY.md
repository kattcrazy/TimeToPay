# Device compatibility

TimeToPay is built for Wear OS 3+ in general, but NFC control and system UI behaviour differ by watch. This page tracks what we know and how you can help fill the gaps.

## Status legend

✅ Tested and working on real hardware

🟡 Likely works but not tested on that exact model

🟠 Work in progress. Partial support or open issue

❓ Unknown... needs a contributor

❌ Known broken or blocked, needs help

## Compatibility Table

| # | Watch | NFC via settings | NFC via adapter | Quick panel package | Wallet reload required | Status |
|---|---|---|---|---|---|---|
| - | Galaxy Watch 7 (SM-L315F) | Setting changes, `mState` may not follow | ✅ `NfcAdapter` reflection | `com.google.android.apps.wearable.systemui` | Yes | ✅ |
| - | Galaxy Watch 6 | Likely same as GW7 | Likely same | Likely same | Likely yes | 🟡 |
| - | Pixel Watch / Pixel Watch 2 | ? | ? | ? | ? | ❓ |
| - | Galaxy Watch 4/5 | ? | ? | ? | ? | ❓ |
| - | Other Wear OS 3+ | ? | ? | ? | ? | ❓ |

## Contributing device info

If your watch isn't listed above, or is listed as 🟡 or ❓, run the probe script and [open a device report issue](https://github.com/kattcrazy/TimeToPay/issues/new?template=device-report.yml).

### Run the probe script

#### Prerequisites

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

If you are also testing the actual app functions as well as providing diagnostics (recommended), complete [installation](INSTALLATION.md).

#### 1. Download the probe script for your platform:

   - [timetopay-probe.ps1](https://raw.githubusercontent.com/kattcrazy/TimeToPay/master/scripts/timetopay-probe.ps1) (Windows)
   - [timetopay-probe.sh](https://raw.githubusercontent.com/kattcrazy/TimeToPay/master/scripts/timetopay-probe.sh) (macOS / Linux)

#### 2. Open a terminal and navigate to the folder that you saved the script in. Then run the script.

Windows (PowerShell):

```powershell
timetopay-probe.ps1
```

macOS / Linux / Git Bash:

```bash
bash timetopay-probe.sh
```

The script prints a report. Copy the full output into your GitHub issue.

### What the script collects

| Step | What it does | Why we need it |
|---|---|---|
| Device | Manufacturer, model, device codename, Android/Wear OS version, SDK | Identifies the exact hardware |
| NFC baseline | `nfc_on` setting and `mState` from `dumpsys nfc` | Starting point before tests |
| NFC method test | Toggles `Settings.Global nfc_on` on/off and re-reads `mState` each time, then restores your original setting | On Samsung, the setting can change while NFC stays off, it's important to know if your watch behaves the same |
| System UI packages | Lists installed `*systemui*` packages | Finds notification shade / quick panel owners |
| Quick panel focus | Pauses so you can open quick settings, then dumps the focused window | Gets the exact overlay package name for your watch |
| TimeToPay setup | Accessibility enabled, `WRITE_SECURE_SETTINGS` granted, recent TimeToPay NFC logs (if installed) | Shows whether setup is complete and if runtime toggling worked |

### After the script - manual checks

The script cannot fully test TimeToPay behaviour by itself. With TimeToPay installed and set up, please also check the following. It's reccomended to have the NFC quick settings tile enabled.

- Open selected wallet app -> NFC turns **on** (check your NFC quick settings tile)
- Leave wallet app -> NFC turns **off**
- Switch back to wallet from recents -> NFC turns **on** again
- Open quick settings while wallet is open -> NFC **stays on**
- Wallet clears any "NFC off" warning after re-entry (by reloading)

Paste results into the issue checkboxes.

### What happens next

1. You open a [device report](https://github.com/kattcrazy/TimeToPay/issues/new?template=device-report.yml) with probe output.
2. A maintainer updates the table and, if needed, makes code changes.
3. Your watch model moves from ❓ to ✅, 🟡, 🟠, or ❌ in the compatibility list.
