@echo off
REM Run database migration to add candidate info and scores to interviews table

echo ========================================
echo Running Interview Database Migration
echo Adding candidate info and scoring fields
echo ========================================
echo.

REM Run the migration SQL script
mysql -u root -p12345 < "%~dp0migrations\add-candidate-info-and-scores.sql"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo Migration completed successfully!
    echo ========================================
    echo.
    echo Added fields:
    echo  - candidate_name
    echo  - candidate_email  
    echo  - job_title
    echo  - technical_score
    echo  - communication_score
    echo  - problem_solving_score
    echo  - cultural_fit_score
    echo  - total_score
    echo.
) else (
    echo.
    echo ========================================
    echo Migration failed!
    echo ========================================
    echo Please check MySQL is running and credentials are correct.
    echo.
)

pause
