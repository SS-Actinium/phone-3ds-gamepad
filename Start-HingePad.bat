@echo off
title Hinge Pad
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start-hinge-pad.ps1"
if errorlevel 1 pause
