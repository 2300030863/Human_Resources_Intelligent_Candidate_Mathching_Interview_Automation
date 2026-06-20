@echo off
REM Quick start script for AI Voice Interview System

echo Starting AI Voice Interview System...
echo.

REM Activate virtual environment
if exist venv\Scripts\activate.bat (
    call venv\Scripts\activate.bat
) else (
    echo ERROR: Virtual environment not found!
    echo Please run setup.bat first
    pause
    exit /b 1
)

REM Check if .env exists
if not exist .env (
    echo ERROR: .env file not found!
    echo Please copy .env.example to .env and configure it
    pause
    exit /b 1
)

echo Starting Streamlit application...
streamlit run frontend/app_new.py

pause
