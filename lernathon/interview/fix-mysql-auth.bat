@echo off
echo Fixing MySQL Authentication Method...
echo.
echo This will change the MySQL root user to use mysql_native_password
echo which is compatible with Python 3.13
echo.

mysql -u root -p12345 < fix-mysql-auth.sql

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ✅ MySQL authentication method updated successfully!
    echo You can now run the interview system.
) else (
    echo.
    echo ❌ Failed to update MySQL authentication.
    echo Please ensure MySQL is running and credentials are correct.
)

pause
