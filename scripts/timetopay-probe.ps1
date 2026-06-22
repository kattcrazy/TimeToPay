# TimeToPay device probe - run with watch connected via ADB.
# Usage: .\timetopay-probe.ps1
#        .\timetopay-probe.ps1 -Auto   # skip quick-panel pause (for scripting)
# Copy the full output into a device report issue.

param(
    [switch]$Auto
)

$ErrorActionPreference = "Stop"

function Write-Section($title) {
    Write-Output ""
    Write-Output "=== $title ==="
}

function Test-AdbReady {
    $state = & adb get-state 2>&1 | Out-String
    $state = $state.Trim()
    if ($state -eq "device") {
        return $true
    }
    return $false
}

function Stop-IfAdbOffline($context) {
    if (Test-AdbReady) {
        return
    }
    Write-Output ""
    Write-Output "ERROR: Watch ADB is offline ($context)."
    Write-Output "Wake your watch, unlock it, and reconnect wireless debugging if needed."
    Write-Output "Then re-run: .\timetopay-probe.ps1"
    exit 1
}

function Invoke-AdbShell {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Command)
    Stop-IfAdbOffline "before adb shell $($Command -join ' ')"
    $output = & adb shell @Command 2>&1
    $text = if ($null -eq $output) {
        ""
    } elseif ($output -is [System.Array]) {
        ($output | ForEach-Object { "$_" }) -join "`n"
    } else {
        "$output"
    }
    if ($text -match "adb\.exe: device offline" -or $text -match "error: device offline") {
        Stop-IfAdbOffline "during adb shell $($Command -join ' ')"
    }
    return $text
}

function Get-MState {
    Stop-IfAdbOffline "before dumpsys nfc"
    $line = adb shell dumpsys nfc 2>$null | Select-String "^mState=" | Select-Object -First 1
    if ($line) { return $line.Line.Trim() }
    return "mState=unknown"
}

function Get-Setting($key) {
    return (Invoke-AdbShell settings get $key).Trim()
}

Write-Section "TIMETOPAY PROBE REPORT"
Write-Output "Generated: $((Get-Date).ToUniversalTime().ToString('yyyy-MM-ddTHH:mm:ssZ'))"

Stop-IfAdbOffline "at start"

Write-Section "DEVICE"
Invoke-AdbShell getprop ro.product.manufacturer
Invoke-AdbShell getprop ro.product.model
Invoke-AdbShell getprop ro.product.device
Invoke-AdbShell getprop ro.build.version.release
Invoke-AdbShell getprop ro.build.version.sdk
Invoke-AdbShell getprop ro.build.display.id

Write-Section "NFC BASELINE"
Write-Output "settings.global.nfc_on=$(Get-Setting 'global nfc_on')"
Write-Output (Get-MState)
adb shell dumpsys nfc 2>$null | Select-String -Pattern "^mAlwaysOnState=|^mScreenState=" | Select-Object -First 5 | ForEach-Object { $_.Line }

Write-Section "NFC METHOD TEST (settings.global.nfc_on)"
$origNfcOn = Get-Setting 'global nfc_on'
$origMstate = Get-MState
Write-Output "Original: nfc_on=$origNfcOn, $origMstate"

Write-Output "--- Enable via settings (nfc_on=1) ---"
Invoke-AdbShell settings put global nfc_on 1 | Out-Null
Start-Sleep -Seconds 2
Write-Output "After enable: nfc_on=$(Get-Setting 'global nfc_on'), $(Get-MState)"

Write-Output "--- Disable via settings (nfc_on=0) ---"
Invoke-AdbShell settings put global nfc_on 0 | Out-Null
Start-Sleep -Seconds 2
Write-Output "After disable: nfc_on=$(Get-Setting 'global nfc_on'), $(Get-MState)"

Write-Output "--- Restore original ---"
if ($origNfcOn -eq "null" -or [string]::IsNullOrWhiteSpace($origNfcOn)) {
    Invoke-AdbShell settings delete global nfc_on 2>$null | Out-Null
} else {
    Invoke-AdbShell settings put global nfc_on $origNfcOn | Out-Null
}
Start-Sleep -Seconds 1
Write-Output "Restored: nfc_on=$(Get-Setting 'global nfc_on'), $(Get-MState)"

Write-Section "SYSTEM UI PACKAGES"
Stop-IfAdbOffline "before system ui package list"
adb shell pm list packages | Select-String -Pattern "systemui" -CaseSensitive:$false | ForEach-Object { $_.Line }

Write-Section "QUICK PANEL FOCUS"
if ($Auto) {
    Write-Output "(Auto mode: skipping quick-panel pause. Open quick settings on watch before running for best results.)"
} else {
    Write-Output "Open quick settings on your watch now, then press Enter..."
    [void][System.Console]::ReadLine()
    Stop-IfAdbOffline "after quick-panel pause (watch may have slept)"
}
adb shell dumpsys window 2>$null | Select-String -Pattern "mCurrentFocus=|mFocusedApp=|StatusBar" | Select-Object -First 10 | ForEach-Object { $_.Line }

Write-Section "TIMETOPAY SETUP"
Stop-IfAdbOffline "before TimeToPay setup check"
$installed = adb shell pm list packages com.timetopay 2>$null | Select-String "package:com\.timetopay"
if ($installed) {
    Write-Output "installed=yes"
    $ver = adb shell dumpsys package com.timetopay 2>$null | Select-String "versionName" | Select-Object -First 1
    if ($ver) { Write-Output $ver.Line.Trim() }
    $a11y = (adb shell settings get secure enabled_accessibility_services 2>$null) -match "com\.timetopay"
    Write-Output "accessibility=$(if ($a11y) { 'enabled' } else { 'not enabled' })"
    $grant = adb shell dumpsys package com.timetopay 2>$null | Select-String "WRITE_SECURE_SETTINGS.*granted=true"
    if ($grant) { Write-Output $grant.Line.Trim() } else { Write-Output "WRITE_SECURE_SETTINGS=not granted" }
    Write-Output "Recent TimeToPay NFC logs:"
    adb logcat -d -s TimeToPay:* 2>$null | Select-Object -Last 15
} else {
    Write-Output "installed=no"
    Write-Output "Install TimeToPay and re-run for runtime NFC logs."
}

Write-Section "END OF REPORT"
Write-Output "Paste everything above into: https://github.com/kattcrazy/TimeToPay/issues/new?template=device-report.yml"
