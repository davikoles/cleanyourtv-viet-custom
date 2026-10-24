param(
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$SdkRoot = $env:ANDROID_HOME
)
$ErrorActionPreference = 'Stop'
$project = Split-Path -Parent $PSScriptRoot
if (-not $JavaHome) {
    $candidate = Get-ChildItem 'C:\Program Files\Microsoft' -Directory -Filter 'jdk-17*' -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($candidate) { $JavaHome = $candidate.FullName }
}
if (-not $JavaHome -or -not (Test-Path "$JavaHome\bin\java.exe")) {
    throw 'Set JAVA_HOME to JDK 17, or pass -JavaHome.'
}
$release = Join-Path $JavaHome 'release'
if (-not (Test-Path $release) -or -not ((Get-Content $release -Raw) -match '(?m)^JAVA_VERSION="17(?:\.|\")')) {
    throw 'This project requires JDK 17. Select a JDK 17 installation.'
}
if (-not $SdkRoot) { $SdkRoot = "$env:LOCALAPPDATA\Android\Sdk" }
foreach ($part in @('platforms\android-34', 'build-tools\34.0.0')) {
    if (-not (Test-Path (Join-Path $SdkRoot $part))) { throw "Install Android SDK component: $part" }
}
$env:JAVA_HOME = $JavaHome
$env:ANDROID_HOME = $SdkRoot
Push-Location (Join-Path $project 'launcher')
try {
    & .\gradlew.bat testReleaseUnitTest lintRelease assembleRelease --no-daemon --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Build/checks failed. Do not install a stale APK.' }
    Write-Host 'Build passed: launcher\app\build\outputs\apk\release\app-release.apk'
} finally { Pop-Location }
