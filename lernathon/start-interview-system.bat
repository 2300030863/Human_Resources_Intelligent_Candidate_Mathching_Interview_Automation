@echo off
REM Start the AI Voice Interview System (Backend + Frontend)

echo ========================================
echo   Starting AI Interview System
echo ========================================
echo.

cd interview

REM Check if virtual environment exists
if not exist venv\Scripts\activate.bat (
    echo ERROR: Virtual environment not found!
    echo Please run: cd interview ^&^& python -m venv venv ^&^& venv\Scripts\activate ^&^& pip install -r requirements.txt
    pause
    exit /b 1
)

REM Check if .env exists
if not exist .env (
    echo WARNING: .env file not found!
    echo Creating .env from template...
    echo GROQ_API_KEY=your_api_key_here > .env
    echo Please edit interview\.env and add your GROQ_API_KEY
)

echo Starting FastAPI Backend (Port 8000)...
start "AI Interview Backend" cmd /k "venv\Scripts\activate.bat && python -m uvicorn backend.main:app --reload --host 0.0.0.0 --port 8000"

timeout /t 3 /nobreak > nul

echo Starting Streamlit Frontend (Port 8501)...
start "AI Interview Frontend" cmd /k "venv\Scripts\activate.bat && streamlit run frontend/app_new.py"

echo.
echo ========================================
echo   AI Interview System Started!
echo ========================================
echo.
echo Backend:  http://localhost:8000
echo Frontend: http://localhost:8501
echo.
echo Press any key to stop all services...
pause > nul

REM Stop services
taskkill /FI "WindowTitle eq AI Interview Backend*" /F /T
taskkill /FI "WindowTitle eq AI Interview Frontend*" /F /T

echo Services stopped.
