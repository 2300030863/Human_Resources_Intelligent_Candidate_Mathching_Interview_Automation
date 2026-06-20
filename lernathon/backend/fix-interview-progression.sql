-- Fix interview progression for completed exams
-- This script will update applications to INTERVIEWING status if the candidate passed the exam

-- First, let's check the exam pass rate for the job and the exam result
SELECT 
    j.title as job_title,
    j.exam_pass_rate,
    ea.id as exam_attempt_id,
    ea.final_score,
    ea.status as exam_status,
    ea.qualified_for_interview,
    a.id as application_id,
    a.status as application_status,
    c.email as candidate_email
FROM exam_attempts ea
JOIN jobs j ON ea.job_id = j.id
JOIN applications a ON ea.application_id = a.id
JOIN candidates c ON ea.candidate_id = c.id
WHERE ea.status = 'COMPLETED'
  AND ea.final_score >= COALESCE(j.exam_pass_rate, 60)
  AND a.status IN ('SUBMITTED', 'SCREENING', 'UNDER_REVIEW')
ORDER BY ea.completed_at DESC;

-- Update applications to INTERVIEWING status where candidate passed
UPDATE applications a
JOIN exam_attempts ea ON a.id = ea.application_id
JOIN jobs j ON ea.job_id = j.id
SET 
    a.status = 'INTERVIEWING',
    a.notes = CONCAT('Auto-progressed to interview: Exam score ', ea.final_score, '%')
WHERE ea.status = 'COMPLETED'
  AND ea.final_score >= COALESCE(j.exam_pass_rate, 60)
  AND a.status IN ('SUBMITTED', 'SCREENING', 'UNDER_REVIEW');

-- Update qualified_for_interview flag on exam attempts
UPDATE exam_attempts ea
JOIN jobs j ON ea.job_id = j.id
SET ea.qualified_for_interview = 1
WHERE ea.status = 'COMPLETED'
  AND ea.final_score >= COALESCE(j.exam_pass_rate, 60)
  AND ea.qualified_for_interview IS NULL;

-- Verify the updates
SELECT 
    j.title as job_title,
    j.exam_pass_rate,
    ea.final_score,
    ea.qualified_for_interview,
    a.status as application_status,
    c.email as candidate_email
FROM exam_attempts ea
JOIN jobs j ON ea.job_id = j.id
JOIN applications a ON ea.application_id = a.id
JOIN candidates c ON ea.candidate_id = c.id
WHERE ea.status = 'COMPLETED'
ORDER BY ea.completed_at DESC
LIMIT 10;
