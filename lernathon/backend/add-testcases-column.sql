-- Add testCases column to questions table for coding question test case validation
USE lernathon_recruitment;

ALTER TABLE questions 
ADD COLUMN test_cases TEXT NULL
COMMENT 'JSON array of test cases for CODING questions - format: [{"input": "...", "expectedOutput": "..."}]';

-- Verify the change
DESCRIBE questions;

SELECT 'Test cases column added successfully to questions table' AS status;
