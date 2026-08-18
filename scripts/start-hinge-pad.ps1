# One-click Hinge Pad server. Double-click Start-HingePad.bat — no commands to type.
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$server = Join-Path $root "pc-server"
Set-Location $server

$py = Get-Command python -ErrorAction SilentlyContinue
if (-not $py) {
    Write-Host "Python is not installed or not on PATH."
    Write-Host "Install from https://www.python.org/downloads/ and tick Add python.exe to PATH."
    exit 1
}

if (-not (Test-Path ".\.venv\Scripts\python.exe")) {
    Write-Host "Creating virtual environment..."
    python -m venv .venv
}
$venvPy = ".\.venv\Scripts\python.exe"
& $venvPy -m pip install --upgrade pip | Out-Null
& $venvPy -m pip install -r requirements.txt
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$desktop = [Environment]::GetFolderPath("Desktop")
$deskBat = Join-Path $desktop "Start Hinge Pad.bat"
$launcher = Join-Path $root "Start-HingePad.bat"
if (-not (Test-Path $deskBat)) {
    Set-Content -Path $deskBat -Value "@echo off`r`ncall `"$launcher`"`r`n"
    Write-Host "Desktop shortcut created: $deskBat"
}

Write-Host ""
Write-Host "This PC LAN addresses:"
Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue |
    Where-Object { $_.IPAddress -notlike "127.*" } |
    ForEach-Object { Write-Host ("  {0}   ({1})" -f $_.IPAddress, $_.InterfaceAlias) }
Write-Host ""
Write-Host "Leave this window open. On the phone: same Wi-Fi, enter an address above, port 26760."
Write-Host "For Azahar Plus pick the 3DS preset. For Arkham Origins pick Xbox."
Write-Host "Ctrl+C stops the server."
Write-Host ""

& $venvPy launcher.py
if ($LASTEXITCODE -ne 0) {
    & $venvPy server.py
}
