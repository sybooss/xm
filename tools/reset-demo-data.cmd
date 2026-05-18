@echo off
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0reset-demo-data.ps1" %*
echo.
pause
