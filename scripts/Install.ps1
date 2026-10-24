param(
    [Parameter(Mandatory = $true)][string]$Serial,
    [string]$Adb = '',
    [string]$Apk = ''
)
$ErrorActionPreference = 'Stop'
$project = Split-Path -Parent $PSScriptRoot
if (-not $Adb) {
    $sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "$env:LOCALAPPDATA\Android\Sdk" }
    $Adb = Join-Path $sdk 'platform-tools\adb.exe'
}
if (-not $Apk) { $Apk = Join-Path $project 'launcher\app\build\outputs\apk\release\app-release.apk' }
if (-not (Test-Path $Adb)) { throw 'ADB was not found. Pass -Adb or set ANDROID_HOME.' }
if (-not (Test-Path $Apk)) { throw 'APK was not found. Run scripts\Build.ps1 first.' }
if ($Serial.Contains(':')) {
    & $Adb connect $Serial
    if ($LASTEXITCODE -ne 0) { throw 'ADB connection failed.' }
}
$state = (& $Adb -s $Serial get-state | Out-String).Trim()
if ($LASTEXITCODE -ne 0 -or $state -ne 'device') { throw 'Device is not ready. Check authorization on the TV.' }
Write-Host 'Install only: this script does not disable stock Home or change default Home.'
Write-Host 'Run this only after a successful build, while unlocked with no child-lock timer.'
& $Adb -s $Serial install -r $Apk
if ($LASTEXITCODE -ne 0) { throw 'Install failed. Do not uninstall or clear data to bypass a signing error.' }
& $Adb -s $Serial shell am start -n com.gathin.tvhome/.MainActivity
if ($LASTEXITCODE -ne 0) { throw 'Could not start Home. Stock Home was not changed.' }
