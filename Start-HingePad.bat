@echo off
title Hinge Pad
cd /d "%~dp0"

if exist "pc-server\.venv\Scripts\pythonw.exe" if exist "pc-server\launcher.py" (
  start "Hinge Pad" /D "%~dp0pc-server" "pc-server\.venv\Scripts\pythonw.exe" "launcher.py"
  exit /b 0
)

echo Setting up Hinge Pad the first time. This window closes when the app opens.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start-hinge-pad.ps1"
if errorlevel 1 pause
