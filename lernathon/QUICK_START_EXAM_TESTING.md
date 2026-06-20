# 🚀 Quick Start: Testing Exam System

## Prerequisites Check

✅ **Backend running**: http://localhost:8089/api  
✅ **Database setup**: recruit_db with exam tables  
✅ **Sample questions loaded**: 11 questions in database  

---

## Test 1: Generate Exam (80% Threshold)

### Request:
```bash
curl -X POST http://localhost:8089/api/exams/generate \
-H "Content-Type: application/json" \
-d '{
  "candidateId": 1,
  "jobId": 1,
  "applicationId": 1,
  "resumeMatchScore": 85.0
}'
```

### PowerShell:
```powershell
$body = @{
    candidateId = 1
    jobId = 1
    applicationId = 1
    resumeMatchScore = 85.0
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8089/api/exams/generate" -Method POST -Body $body -ContentType "application/json"
```

### Expected Response:
```json
{
  "examAttemptId": 1,
  "sessionToken": "a3f12b7c-4d56-4e89-b1c2-3d4e5f6a7b8c",
  "totalQuestions": 13,
  "timeLimit": 60,
  "difficulty": "MEDIUM",
  "message": "Exam generated successfully",
  "questions": [
    {
      "id": 1,
      "skill": "Java",
      "level": "MEDIUM",
      "type": "MCQ",
      "question": "Which keyword is used to declare a constant in Java?",
      "options": ["const", "final", "static", "immutable"],
      "points": 10,
      "timeLimit": 2
    }
  ]
}
```

---

## Test 2: Submit Answer (Correct)

### Request:
```bash
curl -X POST http://localhost:8089/api/exams/submit-answer \
-H "Content-Type: application/json" \
-d '{
  "examAttemptId": 1,
  "questionId": 1,
  "answer": "final",
  "timeTaken": 30,
  "sessionToken": "YOUR_SESSION_TOKEN_HERE"
}'
```

### PowerShell:
```powershell
$body = @{
    examAttemptId = 1
    questionId = 1
    answer = "final"
    timeTaken = 30
    sessionToken = "YOUR_SESSION_TOKEN_HERE"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8089/api/exams/submit-answer" -Method POST -Body $body -ContentType "application/json"
```

### Expected Response:
```json
{
  "isCorrect": true,
  "pointsEarned": 10,
  "explanation": "Correct! The 'final' keyword in Java is used to declare constants.",
  "consecutiveCorrect": 1,
  "consecutiveWrong": 0,
  "difficultyChanged": null,
  "nextQuestion": {...}
}
```

---

## Test 3: Record Cheat Event

### Request:
```bash
curl -X POST http://localhost:8089/api/exams/cheat-event \
-H "Content-Type: application/json" \
-d '{
  "examAttemptId": 1,
  "cheatType": "TAB_SWITCH",
  "sessionToken": "YOUR_SESSION_TOKEN_HERE",
  "metadata": "Switched to Google at 10:30 AM"
}'
```

### PowerShell:
```powershell
$body = @{
    examAttemptId = 1
    cheatType = "TAB_SWITCH"
    sessionToken = "YOUR_SESSION_TOKEN_HERE"
    metadata = "Switched to Google at 10:30 AM"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8089/api/exams/cheat-event" -Method POST -Body $body -ContentType "application/json"
```

### Expected Response (1st cheat):
```json
{
  "currentCheatingScore": 1,
  "warningsRemaining": 2,
  "examTerminated": false,
  "message": "Warning: Cheating detected. You have 2 warnings remaining before exam termination."
}
```

### Expected Response (3rd cheat):
```json
{
  "currentCheatingScore": 3,
  "warningsRemaining": 0,
  "examTerminated": true,
  "message": "Exam has been terminated due to excessive cheating violations."
}
```

---

## Test 4: Get Exam Results

### Request:
```bash
curl http://localhost:8089/api/exams/1/result
```

### PowerShell:
```powershell
Invoke-RestMethod -Uri "http://localhost:8089/api/exams/1/result" -Method GET
```

### Expected Response:
```json
{
  "examAttemptId": 1,
  "status": "COMPLETED",
  "finalScore": 85.5,
  "totalQuestions": 13,
  "answeredQuestions": 10,
  "correctAnswers": 9,
  "cheatingScore": 1,
  "autoSubmitted": false,
  "disqualificationReason": null,
  "answers": [
    {
      "question": "Which keyword is used to declare a constant in Java?",
      "candidateAnswer": "final",
      "correctAnswer": "final",
      "isCorrect": true,
      "pointsEarned": 10
    }
  ]
}
```

---

## Test 5: Check Cheating Score

### Request:
```bash
curl http://localhost:8089/api/exams/1/cheating-score
```

### PowerShell:
```powershell
Invoke-RestMethod -Uri "http://localhost:8089/api/exams/1/cheating-score" -Method GET
```

### Expected Response:
```json
{
  "examAttemptId": 1,
  "cheatingScore": 2,
  "warningsRemaining": 1,
  "cheatEvents": [
    {
      "type": "TAB_SWITCH",
      "timestamp": "2026-02-14T10:30:00",
      "penalty": 1
    },
    {
      "type": "FULLSCREEN_EXIT",
      "timestamp": "2026-02-14T10:35:00",
      "penalty": 1
    }
  ]
}
```

---

## Test 6: Adaptive Difficulty (3 Correct Answers)

Submit 3 consecutive correct answers:

### Answer 1:
```powershell
$body = @{
    examAttemptId = 1
    questionId = 1
    answer = "final"
    timeTaken = 30
    sessionToken = "YOUR_SESSION_TOKEN"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8089/api/exams/submit-answer" -Method POST -Body $body -ContentType "application/json"
```

### Answer 2:
```powershell
$body = @{
    examAttemptId = 1
    questionId = 2
    answer = "O(1)"
    timeTaken = 45
    sessionToken = "YOUR_SESSION_TOKEN"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8089/api/exams/submit-answer" -Method POST -Body $body -ContentType "application/json"
```

### Answer 3 (Triggers Difficulty Upgrade):
```powershell
$body = @{
    examAttemptId = 1
    questionId = 4
    answer = "An array with two elements"
    timeTaken = 25
    sessionToken = "YOUR_SESSION_TOKEN"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8089/api/exams/submit-answer" -Method POST -Body $body -ContentType "application/json"
```

### Expected Response (Answer 3):
```json
{
  "isCorrect": true,
  "pointsEarned": 10,
  "explanation": "Correct!",
  "consecutiveCorrect": 3,
  "consecutiveWrong": 0,
  "difficultyChanged": "Difficulty increased from MEDIUM to HARD",
  "nextQuestion": {
    "level": "HARD"
  }
}
```

---

## Test 7: Frontend Testing (Browser)

### 1. Start Frontend:
```bash
cd frontend
npm install
npm run dev
```

### 2. Navigate to Exam Page:
```
http://localhost:5173/exam/1
```

### 3. Trigger Anti-Cheat Events:
- **Tab Switch**: Press `Ctrl+Tab` or click another tab
- **Fullscreen Exit**: Press `Esc` or F11
- **Copy**: Press `Ctrl+C` (should be blocked)
- **Right Click**: Right-click anywhere (should be blocked)
- **Window Blur**: Click outside browser window

### 4. Expected Behavior:
- Toast notification: "Warning: Cheating detected..."
- Cheat counter increments
- After 3rd cheat: Auto-redirect to results page with "DISQUALIFIED" status

---

## Database Verification

### Check Exam Attempt:
```sql
SELECT id, candidate_id, status, cheating_score, final_score, 
       tab_switch_count, fullscreen_exit_count, auto_submitted
FROM exam_attempts
WHERE id = 1;
```

### Check Cheat Events:
```sql
SELECT exam_attempt_id, cheat_type, penalty_score, timestamp, description
FROM cheat_events
WHERE exam_attempt_id = 1
ORDER BY timestamp;
```

### Check Answers:
```sql
SELECT ea.id, q.question, ea.candidate_answer, ea.is_correct, ea.points_earned
FROM exam_answers ea
JOIN questions q ON ea.question_id = q.id
WHERE ea.exam_attempt_id = 1;
```

### Check Questions:
```sql
SELECT id, skill, level, type, question
FROM questions
WHERE active = 1
ORDER BY level, id;
```

---

## Common Issues

### Issue: "Resume match score must be at least 80%"
**Solution**: Increase `resumeMatchScore` in generate request to ≥ 80.0

### Issue: "Invalid session token"
**Solution**: Use the `sessionToken` returned from generate endpoint

### Issue: "Exam not found"
**Solution**: Verify exam was created with `/generate` endpoint first

### Issue: Backend not responding
**Solution**: 
```powershell
Test-NetConnection -ComputerName localhost -Port 8089
```
If False, restart backend:
```powershell
cd "c:\project\dl project\lernathon\backend"
java -jar target\recruitment-backend-1.0.0.jar
```

---

## Full Test Workflow

```powershell
# 1. Generate Exam
$genBody = @{
    candidateId = 1
    jobId = 1
    applicationId = 1
    resumeMatchScore = 85.0
} | ConvertTo-Json

$exam = Invoke-RestMethod -Uri "http://localhost:8089/api/exams/generate" -Method POST -Body $genBody -ContentType "application/json"
$sessionToken = $exam.sessionToken
$examId = $exam.examAttemptId

Write-Host "Exam generated! ID: $examId, Token: $sessionToken"

# 2. Submit 3 correct answers (trigger difficulty upgrade)
$answerBody = @{
    examAttemptId = $examId
    questionId = 1
    answer = "final"
    timeTaken = 30
    sessionToken = $sessionToken
} | ConvertTo-Json

$result1 = Invoke-RestMethod -Uri "http://localhost:8089/api/exams/submit-answer" -Method POST -Body $answerBody -ContentType "application/json"
Write-Host "Answer 1: Correct=$($result1.isCorrect), Consecutive=$($result1.consecutiveCorrect)"

# 3. Record 3 cheat events (trigger termination)
$cheatTypes = @("TAB_SWITCH", "FULLSCREEN_EXIT", "COPY_ATTEMPT")
foreach ($cheatType in $cheatTypes) {
    $cheatBody = @{
        examAttemptId = $examId
        cheatType = $cheatType
        sessionToken = $sessionToken
        metadata = "Test cheat event"
    } | ConvertTo-Json
    
    $cheatResult = Invoke-RestMethod -Uri "http://localhost:8089/api/exams/cheat-event" -Method POST -Body $cheatBody -ContentType "application/json"
    Write-Host "Cheat $cheatType: Score=$($cheatResult.currentCheatingScore), Terminated=$($cheatResult.examTerminated)"
}

# 4. Get final results
$results = Invoke-RestMethod -Uri "http://localhost:8089/api/exams/$examId/result" -Method GET
Write-Host "Final Status: $($results.status)"
Write-Host "Final Score: $($results.finalScore)"
Write-Host "Cheating Score: $($results.cheatingScore)"
```

---

## Success Indicators

✅ **Exam generated** with sessionToken  
✅ **Answers submitted** with correctness feedback  
✅ **Difficulty upgraded** after 3 correct answers  
✅ **Cheat events recorded** with warnings  
✅ **Exam terminated** after 3 cheating points  
✅ **Results retrieved** with full breakdown  

**All systems operational!** 🎉
