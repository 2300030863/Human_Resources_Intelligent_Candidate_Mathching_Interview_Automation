@echo off
echo ================================================
echo Fix Interview Progression for Passed Candidates
echo ================================================
echo.
echo This script will update applications to INTERVIEWING status
echo for candidates who passed their exams.
echo.
pause

mysql -u root -p12345 recruit_db < fix-interview-progression.sql

echo.
echo ================================================
echo Script execution completed!
echo ================================================
echo.
echo Please refresh your dashboard to see the updates.
echo.
pause
