# AI Voice Interview System Integration

## Overview
The AI Voice Interview System has been integrated with the main recruitment platform. Candidates who pass the coding exam (≥60%) will have an interview automatically scheduled, and they can start the AI-powered voice interview directly from the recruitment system.

---

## Complete Workflow

```
1. Candidate applies for job
   ↓
2. Candidate takes coding exam
   ↓
3. Exam score ≥ 60%?
   YES → Interview automatically scheduled
   ↓
4. Recruiter/Candidate clicks "Start AI Interview"
   ↓
5. AI Interview System opens in new window
   ↓
6. AI conducts voice interview:
   - Generates questions based on job role
   - Records candidate answers
   - Transcribes speech to text
   - Evaluates answers with AI
   - Provides scores and feedback
   ↓
7. Interview results stored in database
   ↓
8. Recruiter can submit interview results
   ↓
9. Application status updates based on results
```

---

## Architecture

### Main Recruitment System (Java/Spring Boot + React)
- **Backend:** Port 8089
- **Frontend:** Port 5173
- **Database:** MySQL (lernathon_recruitment)
- **Purpose:** Job postings, applications, exams, interview scheduling

### AI Interview System (Python/FastAPI + Streamlit)
- **Backend:** Port 8000 (FastAPI)
- **Frontend:** Port 8501 (Streamlit)
- **Database:** MySQL/PostgreSQL (interview_db)
- **Purpose:** AI-powered voice interviews

---

## Setup Instructions

### 1. Install Interview System Dependencies

```bash
cd interview
python -m venv venv
venv\Scripts\activate  # Windows
pip install -r requirements.txt
```

### 2. Configure Environment Variables

Edit `interview/.env`:

```env
# AI API Keys
GROQ_API_KEY=your_groq_api_key_here

# Database Configuration (optional - uses separate DB)
DATABASE_URL=mysql+pymysql://root:password@localhost:3306/interview_db

# Audio Settings
AUDIO_SAMPLE_RATE=16000
AUDIO_MAX_DURATION=300
```

**Get GROQ API Key:**
1. Visit https://console.groq.com/
2. Sign up / Sign in
3. Go to API Keys section
4. Create new API key
5. Copy and paste into `.env`

### 3. Start All Systems

#### Option A: Start Everything Together
```bash
.\start-system.bat  # Starts main backend + frontend + interview system
```

#### Option B: Start Separately
```bash
# Terminal 1: Main Backend
.\restart-backend.bat

# Terminal 2: Main Frontend
cd frontend
npm run dev

# Terminal 3: Interview System
.\start-interview-system.bat
```

---

## Integration Features

### 1. Automatic Interview Scheduling
- When candidate scores ≥60% on exam
- Interview scheduled 2 days in advance
- Status: `SCHEDULED`
- Type: `TECHNICAL`

### 2. Start AI Interview Button
- Appears on SCHEDULED interviews
- Opens interview system in new window
- Passes candidate name and job role
- Prevents popup blocker issues

### 3. Query Parameter Passing
The React app passes data to Streamlit via URL:
```
http://localhost:8501?candidate=John+Doe&job=Software+Engineer&interview_id=123
```

The Streamlit app reads these parameters and pre-fills:
- Candidate name
- Job role
- Interview ID (for linking results)

---

## User Guide

### For Candidates

1. **Take the Exam**
   - Apply for a job
   - Complete the coding exam
   - Score at least 60%

2. **Interview Scheduled**
   - Interview automatically scheduled
   - Check "Interviews" section (if accessible to candidates)
   - Wait for interview time

3. **Start AI Interview** (if button is available)
   - Click "Start AI Interview" button
   - New window opens with AI interview system
   - Allow microphone access when prompted
   - Follow AI interviewer instructions

4. **During Interview**
   - Listen to AI-generated questions
   - Answer using your microphone
   - Or type answers if voice input unavailable
   - AI evaluates answers in real-time
   - Receive immediate feedback

5. **After Interview**
   - View your scores (technical, clarity, communication)
   - Review AI feedback
   - Interview results saved automatically

### For Recruiters

1. **View Scheduled Interviews**
   - Navigate to "Interviews" page
   - See list of all scheduled interviews
   - Filter by status, type, date

2. **Start Interview** (optional)
   - Click "Start AI Interview" to preview
   - Or send link to candidate

3. **Monitor Interview Progress**
   - Interview results stored in database
   - Can view candidate responses
   - See AI evaluation scores

4. **Submit Final Results**
   - Click "Submit Result" after interview
   - Fill in comprehensive feedback form:
     - Status (COMPLETED, NO_SHOW, etc.)
     - Pass/Fail decision
     - Rating (1-5)
     - Recommendation (HIRE, REJECT, NEXT_ROUND)
     - Detailed feedback
     - Strengths and weaknesses
   - Submit to database

5. **Application Updates**
   - If candidate passes: Status → `INTERVIEWING` or `OFFERED`
   - If candidate fails: Status → `REJECTED`
   - All feedback stored for future reference

---

## AI Interview System Features

### Question Generation
- **AI-Powered:** Uses Groq AI (llama-3.1-70b-versatile)
- **Customizable:** Based on job role and skills
- **Adaptive:** Difficulty adjusts based on answers
- **Avoids Repetition:** Tracks previous questions

### Voice Recording
- **Real-time:** Instant recording via browser
- **Multiple Formats:** WAV, MP3 support
- **Quality Control:** 16kHz sample rate
- **Storage:** Audio files saved for review

### Speech-to-Text
- **Technology:** OpenAI Whisper
- **Accuracy:** High-quality transcription
- **Language Support:** Multiple languages
- **Fallback:** Text input option available

### Text-to-Speech
- **AI Voice:** Natural-sounding speech
- **Customizable:** Voice options
- **Accessibility:** Helps candidates understand questions

### AI Evaluation
- **Comprehensive:** Technical, clarity, communication scores
- **Detailed Feedback:** Specific improvement suggestions
- **Fair Scoring:** Consistent evaluation criteria
- **Real-time:** Instant feedback after each answer

### Scoring System
```
Technical Score:     /100 (accuracy, correctness)
Clarity Score:       /100 (coherence, structure)
Communication Score: /100 (articulation, confidence)
────────────────────────────────────────────────
Total Score:         /100 (weighted average)
```

---

## Database Schema

### Main System (MySQL)

**interviews table:**
```sql
id                BIGINT PRIMARY KEY
application_id    BIGINT (FK to applications)
interviewer_id    BIGINT (FK to users)
type              VARCHAR (TECHNICAL, BEHAVIORAL, etc.)
status            VARCHAR (SCHEDULED, COMPLETED, etc.)
scheduled_at      TIMESTAMP
duration_minutes  INTEGER
location          VARCHAR
meeting_link      VARCHAR
feedback          TEXT
rating            INTEGER (1-5)
notes             TEXT
created_at        TIMESTAMP
updated_at        TIMESTAMP
```

### Interview System (MySQL/PostgreSQL)

**interviews table:**
```sql
id                    SERIAL PRIMARY KEY
candidate_name        VARCHAR
job_role              VARCHAR
question              TEXT
answer_transcript     TEXT
technical_score       DECIMAL
clarity_score         DECIMAL
communication_score   DECIMAL
total_score           DECIMAL
feedback              TEXT
audio_file_path       VARCHAR
created_at            TIMESTAMP
```

**interview_sessions table:**
```sql
id                    SERIAL PRIMARY KEY
session_id            VARCHAR UNIQUE
candidate_name        VARCHAR
job_role              VARCHAR
start_time            TIMESTAMP
end_time              TIMESTAMP
total_questions       INTEGER
total_score           DECIMAL
status                VARCHAR
notes                 TEXT
```

---

## API Endpoints

### Main Backend (Port 8089)

**Interviews:**
- `GET /interviews` - List all interviews
- `GET /interviews/{id}` - Get interview details
- `POST /interviews?applicationId={id}` - Create interview
- `PUT /interviews/{id}` - Update interview
- `DELETE /interviews/{id}` - Delete interview
- `POST /interviews/{id}/result` - Submit interview result

### AI Interview Backend (Port 8000)

**Question Generation:**
- `POST /api/generate-question` - Generate new question
  ```json
  {
    "job_role": "Software Engineer",
    "skills": ["Java", "Spring Boot"],
    "difficulty": "medium",
    "previous_questions": []
  }
  ```

**Answer Evaluation:**
- `POST /api/evaluate-answer` - Evaluate candidate answer
  ```json
  {
    "question": "Explain dependency injection...",
    "answer": "Dependency injection is..."
  }
  ```

**Audio Transcription:**
- `POST /api/transcribe-audio` - Convert speech to text
  - Multipart form upload
  - Accepts WAV, MP3, OGG formats

**Interview Storage:**
- `POST /api/interviews` - Save interview result
- `GET /api/interviews` - Get all interviews
- `GET /api/interviews/{id}` - Get specific interview

**Sessions:**
- `POST /api/sessions` - Create interview session
- `GET /api/sessions/{id}` - Get session details

---

## Configuration

### Interview Auto-Scheduling

**Pass Threshold:** 60% (configurable)

Edit `backend/src/main/java/com/lernathon/recruitment/service/ExamSubmissionService.java`:
```java
private void autoProgressToInterview(ExamAttempt exam, double finalScore) {
    if (finalScore >= 60.0 && exam.getApplication() != null) {
        // Change 60.0 to your desired threshold
    }
}
```

**Schedule Delay:** 2 days (configurable)

Edit same file:
```java
interview.setScheduledAt(LocalDateTime.now().plusDays(2)); 
// Change 2 to desired days
```

### Interview System Settings

Edit `interview/config/settings.py`:
```python
# AI Model
AI_MODEL = "llama-3.1-70b-versatile"

# Audio Settings
AUDIO_SAMPLE_RATE = 16000
AUDIO_MAX_DURATION = 300  # 5 minutes max per answer

# Interview Settings
DEFAULT_INTERVIEW_DURATION = 600  # 10 minutes
DEFAULT_TOTAL_QUESTIONS = 8
```

---

## Troubleshooting

### Interview System Won't Start

**Issue:** Virtual environment not found
```bash
cd interview
python -m venv venv
venv\Scripts\activate
pip install -r requirements.txt
```

**Issue:** Missing GROQ_API_KEY
1. Get key from https://console.groq.com/
2. Edit `interview/.env`
3. Add: `GROQ_API_KEY=your_key_here`

**Issue:** Port already in use
```bash
# Find and kill process using port 8000
netstat -ano | findstr :8000
taskkill /PID <PID> /F

# Find and kill process using port 8501
netstat -ano | findstr :8501
taskkill /PID <PID> /F
```

### "Start AI Interview" Button Not Working

**Issue:** Popup blocked
- Allow popups for localhost:5173
- Try Ctrl+Click to open in new tab

**Issue:** Interview system not running
```bash
.\start-interview-system.bat
```

**Issue:** Parameters not passed
- Check browser console for errors
- Verify interview has application with candidate and job info

### Audio Recording Not Working

**Issue:** Microphone permission denied
- Allow microphone access in browser
- Check browser settings → Privacy → Microphone

**Issue:** sounddevice not installed
```bash
cd interview
venv\Scripts\activate
pip install sounddevice soundfile
```

### Evaluation Not Working

**Issue:** GROQ API error
- Verify API key is correct
- Check API rate limits
- Try different AI model in settings

---

## Best Practices

### For Recruiters

1. **Start Interview System Early**
   - Run `.\start-interview-system.bat` at start of day
   - Keep it running during interview hours

2. **Monitor Interview Progress**
   - Check interview results regularly
   - Review AI feedback for consistency

3. **Provide Additional Feedback**
   - AI evaluation is a guide, not final decision
   - Add your own observations in "Notes"

4. **Configure Questions**
   - Review generated questions for relevance
   - Adjust difficulty based on role level

### For Candidates

1. **Test Audio First**
   - Check microphone before interview
   - Speak clearly and at normal pace

2. **Quiet Environment**
   - Minimize background noise
   - Use headphones if possible

3. **Answer Thoroughly**
   - Provide detailed explanations
   - Don't rush through answers

---

## Advanced Features

### Custom Question Templates

Edit `interview/services/ai_service.py` to customize question generation:
```python
def generate_question(self, job_role, skills, difficulty, previous_questions):
    prompt = f"""
    Generate an interview question for a {job_role} position.
    Focus on: {', '.join(skills)}
    Difficulty: {difficulty}
    
    Your custom instructions here...
    """
```

### Video Proctoring (Optional)

Enable in `interview/frontend/app_new.py`:
```python
st.session_state.face_detection_enabled = True
st.session_state.video_enabled = True
```

### Custom Scoring Weights

Edit `interview/services/ai_service.py`:
```python
total_score = (
    technical_score * 0.6 +      # 60% weight
    clarity_score * 0.2 +         # 20% weight
    communication_score * 0.2     # 20% weight
)
```

---

## System URLs

**Main Application:**
- Frontend: http://localhost:5173
- Backend: http://localhost:8089

**AI Interview System:**
- Frontend: http://localhost:8501
- Backend: http://localhost:8000

**Database:**
- Main DB: localhost:3306/lernathon_recruitment
- Interview DB: localhost:3306/interview_db (optional)

---

## Support

### Documentation
- Main System: See README.md files in root
- Interview System: See interview/README.md

### Common Commands

```bash
# Start everything
.\start-system.bat

# Start just interview system
.\start-interview-system.bat

# Rebuild backend
cd backend
mvn clean package -DskipTests

# Restart main backend
.\restart-backend.bat

# Install frontend dependencies
cd frontend
npm install
```

---

## Future Enhancements

- [ ] Direct integration (embed Streamlit in React)
- [ ] Single sign-on between systems
- [ ] Real-time interview monitoring dashboard
- [ ] Advanced analytics and reporting
- [ ] Multi-language support
- [ ] Custom company branding
- [ ] Interview scheduling calendar
- [ ] Email notifications to candidates
- [ ] Interview recording playback
- [ ] AI interview coaching/practice mode

---

## Summary

The AI Voice Interview System is now fully integrated with your recruitment platform:

✅ Automatic interview scheduling after exam pass
✅ One-click launch from Interviews page
✅ Candidate info passed automatically  
✅ AI-powered question generation
✅ Voice recording and transcription
✅ Automated answer evaluation
✅ Comprehensive scoring system
✅ Database storage of all results
✅ Result submission workflow
✅ Application status updates

**Ready to use!** 🚀
