# Build a sideloadable debug APK.
# Requires JDK 17+ and an Android SDK (see README).
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$android = Join-Path $root "android-app"
$dist = Join-Path $root "dist"

if (-not $env:JAVA_HOME) {
    $jdk = Get-ChildItem "C:\Program Files\Microsoft\jdk-17*" -Directory -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending | Select-Object -First 1
    if ($jdk) { $env:JAVA_HOME = $jdk.FullName }
}
if (-not $env:ANDROID_HOME) {
    $sdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"
    if (Test-Path $sdk) { $env:ANDROID_HOME = $sdk; $env:ANDROID_SDK_ROOT = $sdk }
}

$sdkDir = $env:ANDROID_HOME
if (-not $sdkDir -or -not (Test-Path $sdkDir)) {
    throw "ANDROID_HOME is not set and %LOCALAPPDATA%\Android\Sdk was not found."
}

$sdkDirProp = $sdkDir -replace '\\', '/'
Set-Content -Path (Join-Path $android "local.properties") -Value "sdk.dir=$sdkDirProp" -Encoding ASCII

$gradle = Join-Path $env:LOCALAPPDATA "Android\gradle-8.10.2\bin\gradle.bat"
if (-not (Test-Path $gradle)) {
    $wrapper = Join-Path $android "gradlew.bat"
    if (Test-Path $wrapper) { $gradle = $wrapper } else { throw "Gradle 8.10.2 not found." }
}

Push-Location $android
try {
    & $gradle :app:assembleDebug --no-daemon
    if ($LASTEXITCODE -ne 0) { throw "Gradle assembleDebug failed ($LASTEXITCODE)" }
} finally {
    Pop-Location
}

$apk = Join-Path $android "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $apk)) { throw "APK not produced: $apk" }

New-Item -ItemType Directory -Force -Path $dist | Out-Null
$out = Join-Path $dist "HingePad-0.2.1-debug.apk"
Copy-Item $apk $out -Force
Write-Output "APK: $out"
Write-Output ("Size: {0:N1} KB" -f ((Get-Item $out).Length / 1KB))
