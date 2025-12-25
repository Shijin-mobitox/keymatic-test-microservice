@echo off
echo 🚀 Starting Tenant Service...
echo.

REM Run the PowerShell build and run script
powershell.exe -ExecutionPolicy Bypass -File "build-and-run-tenant-service.ps1"

echo.
echo ✅ Script completed. Check output above for any issues.
pause
