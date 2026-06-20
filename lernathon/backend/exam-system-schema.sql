-- Exam System Tables for Role-Based Adaptive Exam with Anti-Cheating

-- Questions Table
CREATE TABLE IF NOT EXISTS questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    skill VARCHAR(100) NOT NULL,
    level VARCHAR(20) NOT NULL,
    type VARCHAR(20) NOT NULL,
    question TEXT NOT NULL,
    code_snippet TEXT,
    options TEXT,
    correct_answer TEXT,
    explanation TEXT,
    points INT DEFAULT 10,
    time_limit INT DEFAULT 120,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_skill (skill),
    INDEX idx_level (level),
    INDEX idx_type (type),
    INDEX idx_active (is_active)
);

-- Exam Attempts Table
CREATE TABLE IF NOT EXISTS exam_attempts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    candidate_id BIGINT NOT NULL,
    job_id BIGINT NOT NULL,
    application_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED',
    current_difficulty VARCHAR(20) NOT NULL,
    cheating_score INT NOT NULL DEFAULT 0,
    consecutive_correct INT NOT NULL DEFAULT 0,
    consecutive_wrong INT NOT NULL DEFAULT 0,
    tab_switch_count INT NOT NULL DEFAULT 0,
    fullscreen_exit_count INT NOT NULL DEFAULT 0,
    copy_paste_attempts INT NOT NULL DEFAULT 0,
    window_blur_count INT NOT NULL DEFAULT 0,
    auto_submitted BOOLEAN NOT NULL DEFAULT FALSE,
    final_score DOUBLE DEFAULT 0.0,
    total_questions INT NOT NULL DEFAULT 0,
    answered_questions INT NOT NULL DEFAULT 0,
    correct_answers INT NOT NULL DEFAULT 0,
    session_token VARCHAR(255),
    ip_address VARCHAR(50),
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    disqualified_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (candidate_id) REFERENCES candidates(id) ON DELETE CASCADE,
    FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE,
    FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE SET NULL,
    INDEX idx_candidate (candidate_id),
    INDEX idx_job (job_id),
    INDEX idx_status (status),
    INDEX idx_session (session_token),
    UNIQUE KEY unique_candidate_job (candidate_id, job_id)
);

-- Exam Answers Table
CREATE TABLE IF NOT EXISTS exam_answers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exam_attempt_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    candidate_answer TEXT,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    points_earned INT NOT NULL DEFAULT 0,
    time_taken INT,
    attempt_number INT DEFAULT 1,
    answered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (exam_attempt_id) REFERENCES exam_attempts(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE,
    INDEX idx_exam_attempt (exam_attempt_id),
    INDEX idx_question (question_id)
);

-- Cheat Events Table
CREATE TABLE IF NOT EXISTS cheat_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exam_attempt_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    penalty_score INT NOT NULL,
    description TEXT,
    metadata TEXT,
    detected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (exam_attempt_id) REFERENCES exam_attempts(id) ON DELETE CASCADE,
    INDEX idx_exam_attempt (exam_attempt_id),
    INDEX idx_type (type),
    INDEX idx_detected (detected_at)
);

-- Insert Sample Questions (for testing)
INSERT INTO questions (skill, level, type, question, options, correct_answer, explanation, points, time_limit) VALUES
-- Java Questions
('Java', 'EASY', 'MCQ', 'What is the correct syntax to output "Hello World" in Java?', 
 '["A) System.out.println(\"Hello World\")", "B) Console.WriteLine(\"Hello World\")", "C) print(\"Hello World\")", "D) echo \"Hello World\""]',
 'A) System.out.println("Hello World")',
 'System.out.println() is the correct method to print output in Java.',
 10, 120),

('Java', 'MEDIUM', 'MCQ', 'Which of the following is not a Java feature?',
 '["A) Object-Oriented", "B) Use of pointers", "C) Platform Independent", "D) Dynamic"]',
 'B) Use of pointers',
 'Java does not support pointers to avoid memory access errors and security issues.',
 10, 120),

('Java', 'HARD', 'CODING', 'Write a Java program to reverse a string without using built-in reverse() method.',
 NULL, NULL,
 'You can use a loop to iterate from the end to the beginning of the string.',
 25, 900),

-- React Questions
('React', 'EASY', 'MCQ', 'What is React?',
 '["A) A JavaScript library for building user interfaces", "B) A database", "C) A server-side framework", "D) A programming language"]',
 'A) A JavaScript library for building user interfaces',
 'React is a JavaScript library developed by Facebook for building user interfaces.',
 10, 120),

('React', 'MEDIUM', 'MCQ', 'What is JSX?',
 '["A) JavaScript XML", "B) Java Syntax Extension", "C) JSON XML", "D) JavaScript Extension"]',
 'A) JavaScript XML',
 'JSX stands for JavaScript XML. It allows us to write HTML in React.',
 10, 120),

('React', 'HARD', 'CODING', 'Create a React component that fetches data from an API and displays it in a list.',
 NULL, NULL,
 'Use useState for state management and useEffect for API calls.',
 25, 900),

-- SQL Questions
('SQL', 'EASY', 'MCQ', 'What does SQL stand for?',
 '["A) Structured Query Language", "B) Simple Query Language", "C) Standard Query Language", "D) System Query Language"]',
 'A) Structured Query Language',
 'SQL stands for Structured Query Language, used to manage relational databases.',
 10, 120),

('SQL', 'MEDIUM', 'MCQ', 'Which SQL statement is used to extract data from a database?',
 '["A) SELECT", "B) GET", "C) EXTRACT", "D) FETCH"]',
 'A) SELECT',
 'SELECT statement is used to query and retrieve data from a database.',
 10, 120),

('SQL', 'HARD', 'CODING', 'Write a SQL query to find the second highest salary from an Employee table.',
 NULL, NULL,
 'Use ORDER BY with LIMIT or use a subquery with MAX().',
 25, 900),

-- Spring Boot Questions
('Spring Boot', 'MEDIUM', 'MCQ', 'What is Spring Boot?',
 '["A) A framework to create stand-alone Spring applications", "B) A database tool", "C) A testing framework", "D) A web browser"]',
 'A) A framework to create stand-alone Spring applications',
 'Spring Boot makes it easy to create stand-alone, production-grade Spring applications.',
 10, 120),

('Spring Boot', 'HARD', 'CODING', 'Create a REST API endpoint in Spring Boot that returns a list of users.',
 NULL, NULL,
 'Use @RestController, @GetMapping, and return a List<User>.',
 25, 900);

COMMIT;
