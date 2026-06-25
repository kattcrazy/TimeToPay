#Requires -Version 5.1
<#
.SYNOPSIS
  Build a release APK signed with the Play Console-registered upload keystore.

.DESCRIPTION
  Uses Android Studio JBR (avoids system Java 26 breaking Gradle).
  Verifies the APK SHA-256 matches the registered upload certificate.
  Does not create or modify upload-keystore.jks or keystore.properties.
#>
$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$ExpectedSha256 = "81F50E3F9244DC6FFC46EA883D1452F95C306850BEAFABBACE7F34611BBB5A29"

function Find-AndroidStudioJbr {
    foreach ($path in @(
            "$env:LOCALAPPDATA\Programs\Android Studio\jbr",
            "$env:ProgramFiles\Android\Android Studio\jbr",
            "$env:ProgramFiles\Android\Android Studio1\jbr"
        )) {
        if (Test-Path (Join-Path $path "bin\java.exe")) { return $path }
    }
    throw "Android Studio JBR not found. Set JAVA_HOME to JDK 17 or 21."
}

function Find-Apksigner {
    $root = Join-Path $env:LOCALAPPDATA "Android\Sdk\build-tools"
    if (-not (Test-Path $root)) { throw "Android SDK build-tools not found at $root" }
    $latest = Get-ChildItem $root -Directory | Sort-Object Name -Descending | Select-Object -First 1
    $bat = Join-Path $latest.FullName "apksigner.bat"
    if (-not (Test-Path $bat)) { throw "apksigner.bat not found under $root" }
    return $bat
}

$keystore = Join-Path $ProjectRoot "upload-keystore.jks"
$props = Join-Path $ProjectRoot "keystore.properties"
if (-not (Test-Path $keystore)) { throw "upload-keystore.jks not found. Do not regenerate - restore your backed-up keystore." }
if (-not (Test-Path $props)) { throw "keystore.properties not found. Copy keystore.properties.example and fill in your existing credentials." }

$env:JAVA_HOME = Find-AndroidStudioJbr
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

Push-Location $ProjectRoot
try {
    & .\gradlew.bat :wear:assembleRelease --no-daemon
    if ($LASTEXITCODE -ne 0) { throw "Gradle build failed with exit code $LASTEXITCODE" }

    $apk = Join-Path $ProjectRoot "wear\build\outputs\apk\release\wear-release.apk"
    if (-not (Test-Path $apk)) { throw "Release APK not found at $apk" }

    $apksigner = Find-Apksigner
    $certLine = & $apksigner verify --print-certs $apk 2>&1 | Select-String "SHA-256 digest:" | Select-Object -First 1
    if (-not $certLine) { throw "Could not read APK certificate from apksigner output." }

    $actual = ($certLine -replace ".*SHA-256 digest:\s*", "").Trim().ToUpper()
    if ($actual -ne $ExpectedSha256) {
        throw "APK SHA-256 ($actual) does not match Play Console registration ($ExpectedSha256). Wrong keystore used."
    }

    Write-Host ""
    Write-Host "Release APK signed with registered upload key." -ForegroundColor Green
    Write-Host "SHA-256: $actual"
    Write-Host "APK: $apk"
} finally {
    Pop-Location
}
