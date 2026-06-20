@echo off
echo ====================================
echo   Restarting Backend Server
echo ====================================
echo.

REM Kill existing Java processes on port 8089
echo [1/3] Stopping old backend...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":8089" ^| findstr "LISTENING"') do (
    echo Found process %%a on port 8089
    taskkill /F /PID %%a >nul 2>&1
)

echo Waiting for port to be released...
timeout /t 3 /nobreak >nul

echo.
echo [2/3] Navigating to backend directory...
cd /d "%~dp0backend"

echo.
echo [3/3] Starting new backend...
echo Backend will start on http://localhost:8089
echo Press Ctrl+C to stop the backend
echo.
echo ====================================
echo.

java -jar target\recruitment-backend-1.0.0.jar

pause
