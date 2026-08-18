@echo off
setlocal
cd /d "%~dp0..\pc-server"
if not exist ".venv\Scripts\python.exe" (
  echo Creating virtualenv...
  python -m venv .venv
  call .venv\Scripts\activate.bat
  python -m pip install --upgrade pip
  python -m pip install -r requirements.txt
) else (
  call .venv\Scripts\activate.bat
)
echo.
echo Hinge Pad server. Ctrl+C to stop.
python server.py %*
