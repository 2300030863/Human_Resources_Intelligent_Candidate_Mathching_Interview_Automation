@echo off
echo Creating recruitment_db database...
echo.

REM Try common MySQL installation paths
set MYSQL_PATH=""

if exist "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" (
    set MYSQL_PATH="C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
)
if exist "C:\Program Files\MySQL\MySQL Server 8.1\bin\mysql.exe" (
    set MYSQL_PATH="C:\Program Files\MySQL\MySQL Server 8.1\bin\mysql.exe"
)
if exist "C:\Program Files\MySQL\MySQL Server 8.2\bin\mysql.exe" (
    set MYSQL_PATH="C:\Program Files\MySQL\MySQL Server 8.2\bin\mysql.exe"
)
if exist "C:\Program Files (x86)\MySQL\MySQL Server 8.0\bin\mysql.exe" (
    set MYSQL_PATH="C:\Program Files (x86)\MySQL\MySQL Server 8.0\bin\mysql.exe"
)

if %MYSQL_PATH%=="" (
    echo ERROR: MySQL executable not found in common paths
    echo Please use MySQL Workbench or find your MySQL installation path
    pause
    exit /b 1
)

echo Found MySQL at: %MYSQL_PATH%
echo.

%MYSQL_PATH% -u root -p12345 -e "CREATE DATABASE IF NOT EXISTS recruitment_db; SHOW DATABASES LIKE 'recruitment_db';"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo SUCCESS! Database 'recruitment_db' has been created.
    echo You can now start the backend with: mvn spring-boot:run
) else (
    echo.
    echo ERROR: Failed to create database. Please check:
    echo 1. MySQL is running
    echo 2. Username 'root' and password '12345' are correct
    echo 3. You have permission to create databases
)

pause
