-- Migration: Add candidate information and detailed scores to interviews table
-- Date: 2026-02-20
-- Description: Adds candidate_name, candidate_email, job_title and detailed scoring fields

USE recruit_db;

-- Add candidate information columns (denormalized for performance)
ALTER TABLE interviews 
ADD COLUMN candidate_name VARCHAR(255) NULL AFTER interviewer_id,
ADD COLUMN candidate_email VARCHAR(255) NULL AFTER candidate_name,
ADD COLUMN job_title VARCHAR(255) NULL AFTER candidate_email;

-- Add detailed scoring columns
ALTER TABLE interviews
ADD COLUMN technical_score INT NULL COMMENT 'Technical knowledge score (0-100)' AFTER rating,
ADD COLUMN communication_score INT NULL COMMENT 'Communication clarity score (0-100)' AFTER technical_score,
ADD COLUMN problem_solving_score INT NULL COMMENT 'Problem solving score (0-100)' AFTER communication_score,
ADD COLUMN cultural_fit_score INT NULL COMMENT 'Cultural fit score (0-100)' AFTER problem_solving_score,
ADD COLUMN total_score INT NULL COMMENT 'Total score (0-100)' AFTER cultural_fit_score;

-- Update existing interviews with candidate information from applications/candidates tables
UPDATE interviews i
JOIN applications a ON i.application_id = a.id
JOIN candidates c ON a.candidate_id = c.id
JOIN jobs j ON a.job_id = j.id
SET 
    i.candidate_name = CONCAT(c.first_name, ' ', c.last_name),
    i.candidate_email = c.email,
    i.job_title = j.title
WHERE i.candidate_name IS NULL;

SELECT 'Migration completed successfully!' as Status;
SELECT 'Added columns: candidate_name, candidate_email, job_title, technical_score, communication_score, problem_solving_score, cultural_fit_score, total_score' as Changes;
