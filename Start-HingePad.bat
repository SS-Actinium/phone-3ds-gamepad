@echo off
cd /d "%~dp0"
if exist "Start-HingePad.vbs" (
  wscript.exe //nologo "%~dp0Start-HingePad.vbs"
  exit /b 0
)
if exist "pc-server\.venv\Scripts\pythonw.exe" (
  cd /d "%~dp0pc-server"
  start "" ".venv\Scripts\pythonw.exe" "launcher.py"
  exit /b 0
)
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start-hinge-pad.ps1"
if errorlevel 1 pause
