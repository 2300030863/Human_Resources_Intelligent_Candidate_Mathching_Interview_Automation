@echo off
echo ========================================
echo MySQL Database Setup for HireAI
echo ========================================
echo.
echo This script will create the recruitment_db database.
echo.
set /p MYSQL_USER="Enter MySQL username (default: root): "
if "%MYSQL_USER%"=="" set MYSQL_USER=root

echo.
echo Creating database 'recruitment_db'...
echo.

mysql -u %MYSQL_USER% -p -e "CREATE DATABASE IF NOT EXISTS recruitment_db; SHOW DATABASES LIKE 'recruitment_db';"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo SUCCESS! Database created successfully!
    echo ========================================
    echo.
    echo Next steps:
    echo 1. Update backend/src/main/resources/application.yml with your MySQL credentials
    echo 2. Run: cd backend ^&^& mvn spring-boot:run
    echo 3. Run: cd frontend ^&^& npm run dev
    echo.
) else (
    echo.
    echo ========================================
    echo ERROR! Failed to create database.
    echo ========================================
    echo.
    echo Please check:
    echo 1. MySQL is installed and running
    echo 2. Your credentials are correct
    echo 3. You have permission to create databases
    echo.
)

pause
