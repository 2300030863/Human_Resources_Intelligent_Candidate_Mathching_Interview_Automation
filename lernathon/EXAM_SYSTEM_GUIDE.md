# Role-Based Adaptive Exam System with Anti-Cheating
## Complete Implementation Guide

## 🎯 Overview

This system automatically generates role-specific technical exams for candidates who achieve ≥80% resume match score. It includes:

- ✅ **AI-Powered Question Generation** using Groq Llama 3.3 70B
- ✅ **Adaptive Difficulty** based on performance
- ✅ **Real-time Anti-Cheat Detection**
- ✅ **Automatic Exam Termination** on cheating threshold breach
- ✅ **Fullscreen Enforcement**
- ✅ **Comprehensive Monitoring** (tab switches, copy/paste, window blur, etc.)

---

## 📋 System Architecture

```
Candidate Applies for Job
         ↓
Resume AI Matching (Groq API)
         ↓
Match Score ≥ 80%?
    ↓ YES          ↓ NO
Generate Exam    Reject
         ↓
Start Exam (Fullscreen + Monitoring)
         ↓
Answer Questions (Adaptive Difficulty)
         ↓
Cheating Detected?
    ↓ YES (Score ≥ 3)     ↓ NO
Auto-Terminate          Continue
         ↓                  ↓
    Disqualified      Complete Exam
                           ↓
                    Evaluate & Score
                           ↓
                    Shortlist / Reject
```

---

## 🗄️ Database Schema

### Questions Table
```sql
CREATE TABLE questions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    skill VARCHAR(100) NOT NULL,           -- e.g., Java, React, SQL
    level VARCHAR(20) NOT NULL,            -- EASY, MEDIUM, HARD, ADVANCED
    type VARCHAR(20) NOT NULL,             -- MCQ, CODING, SCENARIO
    question TEXT NOT NULL,
    code_snippet TEXT,                     -- For coding questions
    options TEXT,                          -- JSON array for MCQ
    correct_answer TEXT,
    explanation TEXT,
    points INT DEFAULT 10,
    time_limit INT DEFAULT 120,            -- seconds
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Exam Attempts Table
```sql
CREATE TABLE exam_attempts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    candidate_id BIGINT NOT NULL,
    job_id BIGINT NOT NULL,
    application_id BIGINT,
    status VARCHAR(20) NOT NULL,           -- NOT_STARTED, IN_PROGRESS, COMPLETED, DISQUALIFIED
    current_difficulty VARCHAR(20) NOT NULL,
    cheating_score INT DEFAULT 0,          -- CRITICAL: Auto-terminate at 3
    consecutive_correct INT DEFAULT 0,
    consecutive_wrong INT DEFAULT 0,
    tab_switch_count INT DEFAULT 0,
    fullscreen_exit_count INT DEFAULT 0,
    copy_paste_attempts INT DEFAULT 0,
    window_blur_count INT DEFAULT 0,
    auto_submitted BOOLEAN DEFAULT FALSE,
    final_score DOUBLE DEFAULT 0.0,
    total_questions INT DEFAULT 0,
    answered_questions INT DEFAULT 0,
    correct_answers INT DEFAULT 0,
    session_token VARCHAR(255),            -- Security token
    ip_address VARCHAR(50),                -- Track IP
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    disqualified_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (candidate_id) REFERENCES candidates(id),
    FOREIGN KEY (job_id) REFERENCES jobs(id),
    FOREIGN KEY (application_id) REFERENCES applications(id)
);
```

### Cheat Events Table
```sql
CREATE TABLE cheat_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exam_attempt_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,             -- TAB_SWITCH, FULLSCREEN_EXIT, etc.
    penalty_score INT NOT NULL,            -- Points deducted
    description TEXT,
    metadata TEXT,                         -- JSON with additional details
    detected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (exam_attempt_id) REFERENCES exam_attempts(id)
);
```

---

## 🚀 Backend Implementation

### Key Components

#### 1. ExamService.java
**Purpose**: Generate AI-powered exams based on job requirements

**Key Methods**:
- `generateExam(ExamGenerationRequest)` - Creates exam using Groq API
- `determineDifficulty(experienceRequired)` - Maps experience to difficulty
- `generateAIQuestions(Job, DifficultyLevel)` - Calls Groq API for questions
- `startExam(examAttemptId, ipAddress)` - Begins exam session

**AI Prompt Structure**:
```java
String prompt = "Generate exam for:
- Job Title: {title}
- Skills: {skills}
- Experience: {years}
- Difficulty: {level}

Generate:
- 10 MCQ (4 options each)
- 2 Coding questions
- 1 Scenario question

Return JSON format with questions, options, answers, explanations."
```

#### 2. CheatDetectionService.java
**Purpose**: Monitor and enforce anti-cheating rules

**Cheating Penalties**:
| Action | Penalty | Auto-Terminate? |
|--------|---------|-----------------|
| Tab Switch | +1 | No |
| Fullscreen Exit | +1 | No |
| Copy/Paste | +1 | No |
| Window Blur | +1 | No |
| Multiple Faces | +3 | **YES** |
| No Face | +2 | No |

**Critical Logic**:
```java
if (cheatingScore >= 3) {
    exam.setStatus(DISQUALIFIED);
    exam.setAutoSubmitted(true);
    examSubmissionService.autoSubmitExam(examId);
    return "EXAM TERMINATED";
}
```

#### 3. ExamSubmissionService.java
**Purpose**: Handle answer submission and adaptive difficulty

**Adaptive Difficulty Rules**:
- ✅ **3 consecutive correct** → Increase difficulty
- ❌ **2 consecutive wrong** → Decrease difficulty

```java
if (consecutiveCorrect >= 3) {
    currentDifficulty = upgradeDifficulty();
    consecutiveCorrect = 0;
}

if (consecutiveWrong >= 2) {
    currentDifficulty = downgradeDifficulty();
    consecutiveWrong = 0;
}
```

---

## 🎨 Frontend Implementation

### Key Components

#### 1. Exam.tsx
**Full-Featured Exam Interface**

**Anti-Cheat Monitoring**:
```typescript
// Tab Switch Detection
useEffect(() => {
  const handleVisibilityChange = () => {
    if (document.hidden) {
      recordCheatEvent("TAB_SWITCH", "User switched tab");
    }
  };
  document.addEventListener("visibilitychange", handleVisibilityChange);
}, []);

// Fullscreen Exit Detection
useEffect(() => {
  const handleFullscreenChange = () => {
    if (!document.fullscreenElement && examStarted) {
      recordCheatEvent("FULLSCREEN_EXIT", "Exited fullscreen");
    }
  };
  document.addEventListener("fullscreenchange", handleFullscreenChange);
}, []);

// Disable Copy/Paste
useEffect(() => {
  const handleCopy = (e) => {
    e.preventDefault();
    recordCheatEvent("COPY_ATTEMPT", "Tried to copy");
  };
  const handlePaste = (e) => {
    e.preventDefault();
    recordCheatEvent("PASTE_ATTEMPT", "Tried to paste");
  };
  document.addEventListener("copy", handleCopy);
  document.addEventListener("paste", handlePaste);
}, []);

// Disable Right-Click
useEffect(() => {
  const handleContextMenu = (e) => {
    e.preventDefault();
    recordCheatEvent("RIGHT_CLICK", "Right-click attempted");
  };
  document.addEventListener("contextmenu", handleContextMenu);
}, []);

// Window Blur Detection
useEffect(() => {
  const handleBlur = () => {
    recordCheatEvent("WINDOW_BLUR", "Window lost focus");
  };
  window.addEventListener("blur", handleBlur);
}, []);

// Disable Developer Tools Shortcuts
useEffect(() => {
  const handleKeyDown = (e) => {
    if (e.key === "F12" || 
        (e.ctrlKey && e.shiftKey && ["I", "J"].includes(e.key)) ||
        (e.ctrlKey && e.key === "U")) {
      e.preventDefault();
      recordCheatEvent("KEYBOARD_SHORTCUT", `Blocked: ${e.key}`);
    }
  };
  document.addEventListener("keydown", handleKeyDown);
}, []);
```

**Recording Cheat Events**:
```typescript
const recordCheatEvent = async (type: string, description: string) => {
  const response = await fetch("/api/exams/cheat-event", {
    method: "POST",
    body: JSON.stringify({
      examAttemptId,
      cheatType: type,
      metadata: JSON.stringify({ description, timestamp: new Date() }),
      sessionToken
    })
  });

  const result = await response.json();

  if (result.examTerminated) {
    // Exam automatically closed
    navigate("/exam-result/" + examAttemptId);
  } else {
    // Show warning
    toast({
      title: "Cheating Warning",
      description: `${result.message} (${result.warningsRemaining} warnings left)`,
      variant: "destructive"
    });
  }
};
```

#### 2. ExamResult.tsx
**Comprehensive Results Dashboard**

Features:
- ✅ Final score and status (Passed/Failed/Disqualified)
- ✅ Question-by-question review
- ✅ Correct vs incorrect answers
- ✅ Cheating score breakdown
- ✅ Disqualification reason (if applicable)

---

## 📡 API Endpoints

### Exam Management
```
POST   /api/exams/generate              - Generate exam for candidate
POST   /api/exams/{id}/start            - Start exam session
POST   /api/exams/submit-answer         - Submit answer for question
POST   /api/exams/{id}/complete         - Complete exam
POST   /api/exams/{id}/auto-submit      - Auto-submit (timeout/cheat)
GET    /api/exams/{id}/result           - Get exam result
```

### Anti-Cheat
```
POST   /api/exams/cheat-event           - Record cheating event
GET    /api/exams/{id}/cheating-score   - Get current cheating score
GET    /api/exams/{id}/status           - Check if disqualified
```

---

## 🔒 Security Features

### 1. Session Token Validation
Every API call requires a valid `sessionToken` that's generated when exam is created.

```java
@PostMapping("/submit-answer")
public ResponseEntity<SubmitAnswerResponse> submitAnswer(@RequestBody SubmitAnswerRequest request) {
    ExamAttempt exam = examService.getExamBySessionToken(request.getSessionToken());
    if (!exam.getId().equals(request.getExamAttemptId())) {
        throw new RuntimeException("Session token mismatch");
    }
    // Process answer...
}
```

### 2. IP Address Logging
```java
String ipAddress = getClientIpAddress(request);
exam.setIpAddress(ipAddress);
```

### 3. Fullscreen Enforcement
```typescript
const startExam = async () => {
  await examContainerRef.current.requestFullscreen();
  setExamStarted(true);
};
```

### 4. One Exam Per Candidate Per Job
```sql
UNIQUE KEY unique_candidate_job (candidate_id, job_id)
```

---

## 🧪 Testing Scenarios

### Test 1: Normal Exam Flow
1. Candidate applies for job
2. Resume match score = 85%
3. System generates exam with 13 questions
4. Candidate completes in fullscreen
5. No cheating detected
6. Final score calculated
7. Status = COMPLETED

### Test 2: Cheating Detection
1. Start exam
2. Switch tab → +1 cheating score
3. Exit fullscreen → +1 cheating score
4. Try copy/paste → +1 cheating score
5. **Total = 3 → EXAM TERMINATED**
6. Status = DISQUALIFIED

### Test 3: Adaptive Difficulty
1. Start at MEDIUM difficulty (3 years experience)
2. Answer 3 questions correctly
3. Difficulty upgraded to HARD
4. Miss 2 questions
5. Difficulty downgraded to MEDIUM

### Test 4: Time Expiry
1. Start exam with 90-minute limit
2. Time runs out
3. Exam auto-submitted
4. Status = COMPLETED
5. autoSubmitted = TRUE

---

## 📝 Configuration

### Groq API Key
Update `application.yml`:
```yaml
groq:
  api:
    key: ${GROQ_API_KEY:your-groq-api-key-here}
```

### Cheating Threshold
Modify in `CheatDetectionService.java`:
```java
private static final int CHEATING_THRESHOLD = 3; // Change to 5 for more lenient
```

### Question Distribution
Modify in `ExamService.java`:
```java
private static final int MCQ_COUNT = 10;
private static final int CODING_COUNT = 2;
private static final int SCENARIO_COUNT = 1;
```

---

## 🚀 Deployment Steps

### 1. Database Setup
```bash
mysql -u root -p recruitment_db < backend/exam-system-schema.sql
```

### 2. Backend Build
```bash
cd backend
mvn clean package -DskipTests
```

### 3. Start Backend
```bash
java -jar target/recruitment-backend-1.0.0.jar
```

### 4. Frontend Development
```bash
cd frontend
npm install
npm run dev
```

---

## 📊 Monitoring & Analytics

### Admin Dashboard Metrics
- Total exams conducted
- Average completion time
- Disqualification rate
- Most common cheat types
- Average scores by skill
- Pass/fail rate

### Query Examples
```sql
-- Disqualification rate
SELECT 
    COUNT(CASE WHEN status = 'DISQUALIFIED' THEN 1 END) * 100.0 / COUNT(*) as disqualification_rate
FROM exam_attempts;

-- Most common cheat types
SELECT type, COUNT(*) as count
FROM cheat_events
GROUP BY type
ORDER BY count DESC;

-- Average score by difficulty
SELECT current_difficulty, AVG(final_score) as avg_score
FROM exam_attempts
WHERE status = 'COMPLETED'
GROUP BY current_difficulty;
```

---

## 🔥 Advanced Features (Future Enhancements)

### 1. Webcam Monitoring
- Face detection using browser MediaStream API
- Multiple faces → immediate disqualification
- No face detected → warning
- Looking away → warning

### 2. Question Randomization
- Shuffle questions per candidate
- Randomize MCQ option order
- Unique coding test cases

### 3. Time-Based Locking
- Lock answers after time expires
- Show time spent per question
- Average time analytics

### 4. Proctoring Dashboard
- Live monitoring of active exams
- Real-time alerts for suspicious activity
- Video recording playback

---

## 📞 Support & Troubleshooting

### Common Issues

**Issue**: Fullscreen doesn't work
**Solution**: Browser security policies - user must initiate fullscreen via button click

**Issue**: Anti-cheat too strict
**Solution**: Adjust `CHEATING_THRESHOLD` from 3 to 5

**Issue**: AI questions generation fails
**Solution**: Check Groq API key and rate limits

**Issue**: Exam not generating for ≥80% match
**Solution**: Check `ApplicationService.java` line 238 - ensure match score calculation is correct

---

## 🎉 Summary

This system provides:
- ✅ **Fully Automated** exam generation
- ✅ **AI-Powered** question creation
- ✅ **Adaptive Testing** based on performance
- ✅ **Enterprise-Grade** anti-cheating
- ✅ **Real-Time Monitoring** and enforcement
- ✅ **Automatic Termination** on cheating threshold breach
- ✅ **Comprehensive Analytics** for HR teams

**Result**: Fair, secure, scalable technical assessment platform for modern recruitment.
