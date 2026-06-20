-- SQL Schema for AI Voice Interview System

-- Create database
CREATE DATABASE IF NOT EXISTS interview_system;
USE interview_system;

-- Interviews table
CREATE TABLE IF NOT EXISTS interviews (
    id INT AUTO_INCREMENT PRIMARY KEY,
    candidate_name VARCHAR(255) NOT NULL,
    job_role VARCHAR(255) NOT NULL,
    question TEXT NOT NULL,
    answer_transcript TEXT,
    technical_score INT DEFAULT 0,
    clarity_score INT DEFAULT 0,
    communication_score INT DEFAULT 0,
    total_score INT DEFAULT 0,
    feedback TEXT,
    audio_file_path VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_candidate_name (candidate_name),
    INDEX idx_job_role (job_role),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Interview sessions table
CREATE TABLE IF NOT EXISTS interview_sessions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(100) NOT NULL UNIQUE,
    candidate_name VARCHAR(255) NOT NULL,
    job_role VARCHAR(255) NOT NULL,
    skills TEXT NOT NULL,
    difficulty_level VARCHAR(50) NOT NULL,
    total_questions INT DEFAULT 0,
    questions_answered INT DEFAULT 0,
    average_score FLOAT DEFAULT 0.0,
    status VARCHAR(50) DEFAULT 'in_progress',
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    INDEX idx_session_id (session_id),
    INDEX idx_candidate_name (candidate_name),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Sample queries

-- Get all interviews for a candidate
-- SELECT * FROM interviews WHERE candidate_name = 'John Doe' ORDER BY created_at DESC;

-- Get average score by job role
-- SELECT job_role, AVG(total_score) as avg_score, COUNT(*) as total_interviews
-- FROM interviews
-- GROUP BY job_role;

-- Get top performers
-- SELECT candidate_name, job_role, AVG(total_score) as avg_score
-- FROM interviews
-- GROUP BY candidate_name, job_role
-- HAVING COUNT(*) >= 3
-- ORDER BY avg_score DESC
-- LIMIT 10;

-- Get interview statistics
-- SELECT 
--     COUNT(*) as total_interviews,
--     AVG(total_score) as avg_score,
--     AVG(technical_score) as avg_technical,
--     AVG(clarity_score) as avg_clarity,
--     AVG(communication_score) as avg_communication
-- FROM interviews;
