-- Add application start and end date columns to jobs table
-- Check and add application_start_date column
ALTER TABLE jobs 
ADD COLUMN application_start_date DATETIME NULL COMMENT 'When job applications can start';

-- Check and add application_end_date column  
ALTER TABLE jobs 
ADD COLUMN application_end_date DATETIME NULL COMMENT 'When job applications will end';

-- Create index for better query performance
CREATE INDEX idx_application_dates ON jobs(application_start_date, application_end_date);

-- Verify columns were added
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_COMMENT 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'jobs' AND COLUMN_NAME IN ('application_start_date', 'application_end_date');

SHOW COLUMNS FROM jobs WHERE Field IN ('application_start_date', 'application_end_date');
