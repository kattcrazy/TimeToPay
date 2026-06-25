#Requires -Version 5.1
<#
.SYNOPSIS
  Upload TimeToPay signing credentials to GitHub Actions secrets (one-time setup).

.DESCRIPTION
  Reads your local upload-keystore.jks and keystore.properties.
  Does not modify or regenerate the keystore.
  Requires: GitHub CLI (gh) logged in with repo access.

  Run once after: gh auth login
#>
$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$gh = "C:\Program Files\GitHub CLI\gh.exe"
if (-not (Test-Path $gh)) { $gh = "gh" }

function Require-GhAuth {
    & $gh auth status 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "GitHub CLI is not logged in." -ForegroundColor Yellow
        Write-Host "Run: gh auth login" -ForegroundColor Yellow
        Write-Host "  -> GitHub.com, HTTPS, Login with a web browser" -ForegroundColor Yellow
        throw "gh auth required"
    }
}

$keystorePath = Join-Path $ProjectRoot "upload-keystore.jks"
$propsPath = Join-Path $ProjectRoot "keystore.properties"
if (-not (Test-Path $keystorePath)) { throw "upload-keystore.jks not found at $keystorePath" }
if (-not (Test-Path $propsPath)) { throw "keystore.properties not found at $propsPath" }

$props = @{}
Get-Content $propsPath | ForEach-Object {
    if ($_ -match '^([^#=]+)=(.*)$') { $props[$matches[1].Trim()] = $matches[2].Trim() }
}
foreach ($key in @("storePassword", "keyAlias", "keyPassword")) {
    if (-not $props[$key]) { throw "keystore.properties is missing $key" }
}

Require-GhAuth
Push-Location $ProjectRoot
try {
    $base64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($keystorePath))
    $base64File = Join-Path $env:TEMP "timetopay-keystore.b64"
    [IO.File]::WriteAllText($base64File, $base64, (New-Object System.Text.UTF8Encoding $false))

    Write-Host "Setting GitHub secrets on kattcrazy/TimeToPay..." -ForegroundColor Cyan
    Get-Content $base64File -Raw | & $gh secret set UPLOAD_KEYSTORE_BASE64
    if ($LASTEXITCODE -ne 0) { throw "Failed to set UPLOAD_KEYSTORE_BASE64" }

    $props["storePassword"] | & $gh secret set UPLOAD_KEYSTORE_PASSWORD
    if ($LASTEXITCODE -ne 0) { throw "Failed to set UPLOAD_KEYSTORE_PASSWORD" }

    $props["keyAlias"] | & $gh secret set UPLOAD_KEY_ALIAS
    if ($LASTEXITCODE -ne 0) { throw "Failed to set UPLOAD_KEY_ALIAS" }

    $props["keyPassword"] | & $gh secret set UPLOAD_KEY_PASSWORD
    if ($LASTEXITCODE -ne 0) { throw "Failed to set UPLOAD_KEY_PASSWORD" }

    Write-Host ""
    Write-Host "Done. GitHub Actions can now sign releases with your Play Console key." -ForegroundColor Green
    Write-Host "Secrets set: UPLOAD_KEYSTORE_BASE64, UPLOAD_KEYSTORE_PASSWORD, UPLOAD_KEY_ALIAS, UPLOAD_KEY_PASSWORD"
} finally {
    Pop-Location
    if (Test-Path $base64File) { Remove-Item $base64File -Force }
}
