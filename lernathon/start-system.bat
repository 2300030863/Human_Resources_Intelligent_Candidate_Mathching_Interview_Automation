@echo off
echo ====================================
echo   Starting Recruitment System
echo ====================================
echo.

REM Check if backend JAR exists
if not exist "backend\target\recruitment-backend-1.0.0.jar" (
    echo ERROR: Backend JAR not found!
    echo Please build the backend first:
    echo   cd backend
    echo   mvn clean package -DskipTests
    pause
    exit /b 1
)

echo [1/3] Starting Backend Server...
echo.
start "Backend Server" cmd /k "cd /d "%~dp0" && call restart-backend.bat"

echo Waiting for backend to start...
timeout /t 15 /nobreak >nul

echo.
echo [2/3] Checking Backend Status...
curl -s http://localhost:8089/api/health >nul 2>&1
if %errorlevel%==0 (
    echo Backend is READY!
) else (
    echo Backend is starting... (may take a few more seconds)
)

echo.
echo [3/3] Starting Frontend...
echo.
start "Frontend Server" cmd /k "cd /d "%~dp0frontend" && npm run dev"

echo.
echo ====================================
echo   System Started Successfully!
echo ====================================
echo.
echo Backend:  http://localhost:8089
echo Frontend: http://localhost:5173
echo.
echo Two terminal windows have opened:
echo   1. Backend Server (Java)
echo   2. Frontend Server (Vite)
echo.
echo Press Ctrl+C in each window to stop the servers
echo.
pause
