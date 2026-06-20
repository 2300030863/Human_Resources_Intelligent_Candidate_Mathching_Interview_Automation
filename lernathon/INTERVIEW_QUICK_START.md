# Quick Start Guide - Interview System

## Prerequisites
- MySQL database `recruit_db` running on localhost:3306
- At least one application record in the database
- Python environment with dependencies installed

## Step-by-Step Instructions

### 1. Test the Integration

Run the test script to verify everything is working:

```bash
cd "c:\project\dl project\lernathon"
python test-interview-integration.py
```

**Expected Output:**
- ✅ Database connection successful
- ✅ Application found
- ✅ Interview created successfully
- ✅ Interview updated successfully
- ✅ Interview query successful
- ✅ ALL TESTS PASSED!

### 2. Start the Interview System

**Option A: Frontend Only (Recommended)**
```bash
cd "c:\project\dl project\lernathon\interview"
.venv\Scripts\activate
streamlit run frontend/app.py
```

**Option B: Backend + Frontend (Full API)**
Terminal 1 - Backend:
```bash
cd "c:\project\dl project\lernathon\interview"
.venv\Scripts\activate
python backend/main.py
```

Terminal 2 - Frontend:
```bash
cd "c:\project\dl project\lernathon\interview"
.venv\Scripts\activate
streamlit run frontend/app.py
```

### 3. Conduct an Interview

1. **Open Browser**: Navigate to http://localhost:8501

2. **Enter Application ID**: 
   - In the sidebar, enter an application ID (e.g., 1)
   - Click "🔍 Fetch Candidate Details"
   - Verify candidate information appears

3. **Configure Settings**:
   - Select difficulty level
   - Choose number of questions (1-10)
   - Enable/disable video recording
   - Enable fullscreen mode (recommended)

4. **Start Interview**:
   - Click "🚀 Start Interview"
   - System creates interview record in database

5. **Ask Questions**:
   - Click "📝 Next Question"
   - AI generates and speaks the question
   - Wait for candidate to think

6. **Record Answers**:
   - Click "🔴 Start Recording"
   - Speak clearly for the selected duration
   - System transcribes and evaluates automatically

7. **Review Evaluation**:
   - View scores (Technical, Clarity, Communication)
   - Read AI feedback

8. **Continue**:
   - Repeat steps 5-7 for all questions

9. **Complete**:
   - After final question, view summary
   - System automatically saves results to database

### 4. View Results

**In the Application:**
- Results are displayed at the end of interview
- Shows total score, average, and assessment

**In the Database:**
```sql
-- View latest interview
SELECT 
    i.id,
    i.status,
    i.rating,
    i.feedback,
    c.first_name,
    c.last_name,
    j.title
FROM interviews i
JOIN applications a ON i.application_id = a.id
JOIN candidates c ON a.candidate_id = c.id
JOIN jobs j ON a.job_id = j.id
ORDER BY i.created_at DESC
LIMIT 1;

-- View Q&A details
SELECT notes FROM interviews WHERE id = 1;  -- Replace with interview ID
```

## Troubleshooting

### "Application not found"
- Check application exists: `SELECT * FROM applications;`
- Verify application_id is correct
- Ensure candidate and job exist in database

### "Database connection failed"
- Start MySQL: Check services or start manually
- Verify credentials: root/12345
- Check database exists: `SHOW DATABASES;`

### "Error creating interview"
- Check foreign key constraints
- Ensure application_id is valid
- Verify MySQL is accepting connections

### Audio/Transcription Issues
- Check microphone permissions
- Ensure audio is clear
- Try shorter recording duration
- Check internet connection (for API calls)

## Tips for Best Results

1. **Quiet Environment**: Conduct interviews in a quiet room
2. **Good Microphone**: Use a quality microphone for clear audio
3. **Speak Clearly**: Enunciate words for better transcription
4. **Adequate Time**: Allow 3-5 minutes per question
5. **Test First**: Run test interview before actual candidates

## What's Stored in Database

**Interview Record (`interviews` table):**
- `application_id`: Links to candidate application
- `type`: TECHNICAL (default)
- `status`: SCHEDULED → COMPLETED
- `rating`: Average score (1-10)
- `feedback`: Summary and assessment
- `notes`: Complete Q&A transcript with evaluations
- `created_at`: Interview start time
- `updated_at`: Last update time

**Interview Notes Format:**
```
--- Question 1 ---
Q: What is polymorphism in OOP?
A: Polymorphism is the ability of objects...
Score: 8/10 (Technical: 4/5, Clarity: 2/3, Communication: 2/2)
Feedback: Excellent explanation with good examples

--- Question 2 ---
...

=== INTERVIEW SUMMARY ===
Candidate: John Doe
Position: Software Engineer
Total Questions: 5
Total Score: 42/50
Average Score: 8.40/10
Assessment: Excellent performance - Highly recommended for next round
```

## Next Steps After Interview

1. **Review Results**: Check interview notes in database
2. **Make Decision**: Use rating and assessment for hiring decision
3. **Update Application Status**: Move to next stage if passed
4. **Schedule Next Round**: If applicable, schedule next interview
5. **Send Feedback**: Provide feedback to candidate (if required)

## Integration with Recruitment System

The interview results are automatically available in the main recruitment system:

1. Java backend can query interview results
2. Application status can be updated based on rating
3. Interview history is maintained for reporting
4. Audit trail is complete with timestamps

---

**For technical support, see:** [INTERVIEW_SYSTEM_INTEGRATION.md](INTERVIEW_SYSTEM_INTEGRATION.md)
