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
$deskVbs = Join-Path $desktop "Start Hinge Pad.vbs"
$srcVbs = Join-Path $root "Start-HingePad.vbs"
if (Test-Path $srcVbs) {
    Copy-Item $srcVbs $deskVbs -Force
}

$pythonw = ".\.venv\Scripts\pythonw.exe"
if (Test-Path $pythonw) {
    Start-Process -FilePath (Resolve-Path $pythonw) -ArgumentList "launcher.py" -WorkingDirectory $server
} else {
    & $venvPy launcher.py
}
