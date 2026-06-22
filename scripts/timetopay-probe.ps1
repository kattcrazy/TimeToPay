# TimeToPay device probe - run with watch connected via ADB.
# Usage: .\scripts\timetopay-probe.ps1
# Copy the full output into a device report issue.

$ErrorActionPreference = "Continue"

function Write-Section($title) {
    Write-Output ""
    Write-Output "=== $title ==="
}

function Get-MState {
    $line = adb shell dumpsys nfc 2>$null | Select-String "^mState=" | Select-Object -First 1
    if ($line) { return $line.Line.Trim() }
    return "mState=unknown"
}

function Invoke-Adb([string[]]$Args) {
    & adb @Args
}

Write-Section "TIMETOPAY PROBE REPORT"
Write-Output "Generated: $((Get-Date).ToUniversalTime().ToString('yyyy-MM-ddTHH:mm:ssZ'))"

Write-Section "DEVICE"
Invoke-Adb shell getprop ro.product.manufacturer
Invoke-Adb shell getprop ro.product.model
Invoke-Adb shell getprop ro.product.device
Invoke-Adb shell getprop ro.build.version.release
Invoke-Adb shell getprop ro.build.version.sdk
Invoke-Adb shell getprop ro.build.display.id

Write-Section "NFC BASELINE"
Write-Output "settings.global.nfc_on=$((Invoke-Adb shell settings get global nfc_on).Trim())"
Write-Output (Get-MState)
adb shell dumpsys nfc 2>$null | Select-String -Pattern "^mAlwaysOnState=|^mScreenState=" | Select-Object -First 5 | ForEach-Object { $_.Line }

Write-Section "NFC METHOD TEST (settings.global.nfc_on)"
$origNfcOn = (Invoke-Adb shell settings get global nfc_on).Trim()
$origMstate = Get-MState
Write-Output "Original: nfc_on=$origNfcOn, $origMstate"

Write-Output "--- Enable via settings (nfc_on=1) ---"
Invoke-Adb shell settings put global nfc_on 1 | Out-Null
Start-Sleep -Seconds 2
Write-Output "After enable: nfc_on=$((Invoke-Adb shell settings get global nfc_on).Trim()), $(Get-MState)"

Write-Output "--- Disable via settings (nfc_on=0) ---"
Invoke-Adb shell settings put global nfc_on 0 | Out-Null
Start-Sleep -Seconds 2
Write-Output "After disable: nfc_on=$((Invoke-Adb shell settings get global nfc_on).Trim()), $(Get-MState)"

Write-Output "--- Restore original ---"
if ($origNfcOn -eq "null" -or [string]::IsNullOrWhiteSpace($origNfcOn)) {
    Invoke-Adb shell settings delete global nfc_on 2>$null | Out-Null
} else {
    Invoke-Adb shell settings put global nfc_on $origNfcOn | Out-Null
}
Start-Sleep -Seconds 1
Write-Output "Restored: nfc_on=$((Invoke-Adb shell settings get global nfc_on).Trim()), $(Get-MState)"

Write-Section "SYSTEM UI PACKAGES"
adb shell pm list packages | Select-String -Pattern "systemui" -CaseSensitive:$false | ForEach-Object { $_.Line }

Write-Section "QUICK PANEL FOCUS"
Write-Output "Open quick settings on your watch now, then press Enter..."
[void][System.Console]::ReadLine()
adb shell dumpsys window 2>$null | Select-String -Pattern "mCurrentFocus=|mFocusedApp=|StatusBar" | Select-Object -First 10 | ForEach-Object { $_.Line }

Write-Section "TIMETOPAY SETUP"
$installed = adb shell pm list packages com.timetopay 2>$null | Select-String "timetopay"
if ($installed) {
    Write-Output "installed=yes"
    $ver = adb shell dumpsys package com.timetopay 2>$null | Select-String "versionName" | Select-Object -First 1
    Write-Output $ver.Line.Trim()
    $a11y = (adb shell settings get secure enabled_accessibility_services 2>$null) -match "com\.timetopay"
    Write-Output "accessibility=$(if ($a11y) { 'enabled' } else { 'not enabled' })"
    $grant = adb shell dumpsys package com.timetopay 2>$null | Select-String "WRITE_SECURE_SETTINGS" -Context 0,1
    Write-Output $grant.Line.Trim()
    Write-Output "Recent TimeToPay NFC logs:"
    adb logcat -d -s TimeToPay:* 2>$null | Select-Object -Last 15
} else {
    Write-Output "installed=no"
    Write-Output "Install TimeToPay and re-run for runtime NFC logs."
}

Write-Section "END OF REPORT"
Write-Output "Paste everything above into: https://github.com/kattcrazy/TimeToPay/issues/new?template=device-report.yml"
