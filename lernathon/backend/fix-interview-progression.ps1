# Fix interview progression for existing completed exams
# This script will update applications and create interviews for candidates who passed

$mysqlPath = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
if (-not (Test-Path $mysqlPath)) {
    $mysqlPath = "C:\Program Files\MySQL\MySQL Server 5.7\bin\mysql.exe"
}
if (-not (Test-Path $mysqlPath)) {
    Write-Host "MySQL not found. Please run the SQL commands manually."
    Write-Host "Press any key to see the SQL commands..."
    $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
    Get-Content "fix-interview-progression.sql"
    exit
}

Write-Host "================================================"
Write-Host "Fix Interview Progression for Passed Candidates"
Write-Host "================================================"
Write-Host ""

# First, check what needs to be fixed
Write-Host "Checking for exams that need interview progression..."
& $mysqlPath -u root -p12345 recruit_db -e @"
SELECT 
    j.title as job_title,
    COALESCE(j.exam_pass_rate, 60) as pass_rate_required,
    ea.final_score,
    a.status as current_status,
    c.email
FROM exam_attempts ea
JOIN jobs j ON ea.job_id = j.id
JOIN applications a ON ea.application_id = a.id  
JOIN candidates c ON ea.candidate_id = c.id
WHERE ea.status = 'COMPLETED'
  AND ea.final_score >= COALESCE(j.exam_pass_rate, 60)
  AND a.status IN ('SUBMITTED', 'SCREENING', 'UNDER_REVIEW')
ORDER BY ea.completed_at DESC;
"@

Write-Host ""
Write-Host "Updating applications to INTERVIEWING status..."

& $mysqlPath -u root -p12345 recruit_db -e @"
UPDATE applications a
JOIN exam_attempts ea ON a.id = ea.application_id
JOIN jobs j ON ea.job_id = j.id
SET 
    a.status = 'INTERVIEWING',
    a.notes = CONCAT('Manually progressed to interview: Exam score ', ea.final_score, '%')
WHERE ea.status = 'COMPLETED'
  AND ea.final_score >= COALESCE(j.exam_pass_rate, 60)
  AND a.status IN ('SUBMITTED', 'SCREENING', 'UNDER_REVIEW');

UPDATE exam_attempts ea
JOIN jobs j ON ea.job_id = j.id
SET ea.qualified_for_interview = 1
WHERE ea.status = 'COMPLETED'
  AND ea.final_score >= COALESCE(j.exam_pass_rate, 60);
"@

Write-Host "Creating interview records..."

& $mysqlPath -u root -p12345 recruit_db -e @"
INSERT INTO interviews (application_id, type, status, scheduled_at, duration_minutes, notes, created_at)
SELECT 
    a.id,
    'TECHNICAL',
    'SCHEDULED',
    DATE_ADD(NOW(), INTERVAL 2 DAY),
    60,
    CONCAT('Auto-scheduled after passing exam with score ', ea.final_score, '%'),
    NOW()
FROM applications a
JOIN exam_attempts ea ON ea.application_id = a.id
WHERE a.status = 'INTERVIEWING'
  AND NOT EXISTS (
      SELECT 1 FROM interviews i WHERE i.application_id = a.id
  );
"@

Write-Host ""
Write-Host "================================================"
Write-Host "Complete! Checking results..."
Write-Host "================================================"
Write-Host ""

& $mysqlPath -u root -p12345 recruit_db -e @"
SELECT 
    j.title,
    c.email,
    ea.final_score,
    a.status as app_status,
    i.type as interview_type,
    i.status as interview_status,
    i.scheduled_at
FROM applications a
JOIN exam_attempts ea ON ea.application_id = a.id
JOIN jobs j ON ea.job_id = j.id
JOIN candidates c ON ea.candidate_id = c.id
LEFT JOIN interviews i ON i.application_id = a.id
WHERE ea.status = 'COMPLETED'
ORDER BY ea.completed_at DESC
LIMIT 5;
"@

Write-Host ""
Write-Host "Done! Please refresh your candidate dashboard."
Write-Host ""
pause
