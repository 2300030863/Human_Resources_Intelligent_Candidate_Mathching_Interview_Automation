@echo off
echo Running database migration to add test_cases column...
echo.

mysql -u root -proot -D lernathon_recruitment < add-testcases-column.sql

if %errorlevel% equ 0 (
    echo Migration completed successfully!
) else (
    echo Migration failed. Please run manually:
    echo mysql -u root -proot -D lernathon_recruitment ^< add-testcases-column.sql
)

pause
