-- Add exam and interview configuration fields to jobs table

USE lernathon_recruitment;

-- Add exam pass rate column
ALTER TABLE jobs 
ADD COLUMN exam_pass_rate DOUBLE DEFAULT 60.0 
COMMENT 'Minimum exam pass rate required (0-100)';

-- Add interview pass rate column
ALTER TABLE jobs 
ADD COLUMN interview_pass_rate DOUBLE DEFAULT 70.0 
COMMENT 'Minimum interview pass rate required (0-100)';

-- Add interview question mode column
ALTER TABLE jobs 
ADD COLUMN interview_question_mode VARCHAR(20) DEFAULT 'INTERMEDIATE' 
COMMENT 'Interview question difficulty level: BASIC, INTERMEDIATE, ADVANCED';

-- Update existing jobs with default values if needed
UPDATE jobs 
SET exam_pass_rate = 60.0 
WHERE exam_pass_rate IS NULL;

UPDATE jobs 
SET interview_pass_rate = 70.0 
WHERE interview_pass_rate IS NULL;

UPDATE jobs 
SET interview_question_mode = 'INTERMEDIATE' 
WHERE interview_question_mode IS NULL;

SELECT 'Job exam and interview fields added successfully!' as Status;
