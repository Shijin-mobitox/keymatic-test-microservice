@echo off
echo 🚀 Starting Keycloak Setup...
echo.

REM Run the PowerShell script
powershell.exe -ExecutionPolicy Bypass -File "setup-cloud-keycloak-complete.ps1"

echo.
echo 🎉 Setup completed! Check the output above for any issues.
pause
