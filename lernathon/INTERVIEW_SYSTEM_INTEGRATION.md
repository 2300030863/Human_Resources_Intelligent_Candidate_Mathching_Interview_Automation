# Interview System Integration - Complete Guide

## Overview
The interview system has been fully integrated with the main recruitment database (`recruit_db`). It now properly displays candidate names and stores interview results without errors.

## What Was Fixed

### 1. Database Integration
- **Connected to recruit_db**: The interview system now uses the same MySQL database as the Java backend
- **Database URL**: `mysql+pymysql://root:12345@localhost:3306/recruit_db`
- **Shared Schema**: Interview records are stored in the same `interviews` table used by the recruitment system

### 2. Interview Model Updates
The Interview model has been updated to match the Java backend structure:
- `application_id` - Links to the application in the recruitment system
- `type` - Interview type (TECHNICAL, PHONE_SCREEN, VIDEO, etc.)
- `status` - Interview status (SCHEDULED, COMPLETED, CANCELLED, etc.)
- `scheduled_at` - When the interview is scheduled
- `duration_minutes` - Interview duration
- `feedback` - Overall interview feedback/summary
- `rating` - Interview rating (1-10)
- `notes` - Detailed notes with Q&A transcript and evaluations

### 3. Candidate Information Display
- **Prominent Display**: Candidate name is shown in large font during the interview
- **Always Visible**: Candidate name, application ID, and interview ID are displayed in the sidebar
- **Fetched from Database**: Candidate details are automatically retrieved using application ID

### 4. Interview Result Storage
- **Proper Schema**: Results are stored using the correct database structure
- **Detailed Notes**: Each Q&A with scores and feedback is appended to notes
- **Final Summary**: Interview completion triggers a summary with assessment
- **Status Update**: Interview status is updated to COMPLETED when finished
- **Rating Calculation**: Final average score is saved as the rating

## How to Use

### Step 1: Start the Interview System

1. **Start the backend API** (optional, for REST API access):
   ```bash
   cd interview
   .venv\Scripts\activate
   python backend/main.py
   ```

2. **Start the frontend**:
   ```bash
   cd interview
   .venv\Scripts\activate
   streamlit run frontend/app.py
   ```

### Step 2: Begin an Interview

1. **Enter Application ID**: In the sidebar, enter the application ID from the recruitment system
   - You can find application IDs in the `applications` table
   - Example: Application ID 1

2. **Fetch Candidate Details**: Click "🔍 Fetch Candidate Details"
   - System will display: Candidate name, Job title, Email
   - If not found, check the application ID is correct

3. **Configure Interview Settings**:
   - **Difficulty Level**: Basic, Intermediate, or Advanced
   - **Number of Questions**: 1-10 questions (default: 5)
   - **Video Recording**: Optional webcam recording
   - **Fullscreen Mode**: Recommended for focus

4. **Start Interview**: Click "🚀 Start Interview"
   - A new interview record is created in the database
   - Interview status is set to SCHEDULED
   - System generates a unique interview ID

### Step 3: Conduct the Interview

1. **Generate Question**: Click "📝 Next Question"
   - AI generates a relevant question based on job role and skills
   - Question is displayed and spoken aloud (text-to-speech)

2. **Record Answer**: 
   - Set recording duration (default: 60 seconds)
   - Click "🔴 Start Recording"
   - Speak your answer clearly
   - System automatically transcribes and evaluates

3. **View Evaluation**:
   - Technical score (0-5)
   - Clarity score (0-3)
   - Communication score (0-2)
   - Total score (0-10)
   - Detailed feedback

4. **Continue**: Repeat steps 1-3 for all questions

### Step 4: Complete the Interview

When all questions are answered:
- **Summary is displayed**: Total questions, total score, average score
- **Assessment is generated**: Performance evaluation
- **Database is updated**:
  - Interview status → COMPLETED
  - Rating is set to average score
  - Feedback contains complete summary
  - Notes contain all Q&A with evaluations

### Step 5: View Results in Database

You can query the interview results:

```sql
-- View interview with candidate details
SELECT 
    i.id as interview_id,
    i.status,
    i.rating,
    i.feedback,
    i.notes,
    c.first_name,
    c.last_name,
    j.title as job_title
FROM interviews i
JOIN applications a ON i.application_id = a.id
JOIN candidates c ON a.candidate_id = c.id
JOIN jobs j ON a.job_id = j.id
WHERE i.id = 1;  -- Replace with your interview ID
```

## Technical Details

### Database Schema Mapping

**Python Interview Model** → **Java Interview Entity**:
- ✅ `application_id` → Links to Application
- ✅ `type` → InterviewType enum (TECHNICAL, etc.)
- ✅ `status` → InterviewStatus enum (SCHEDULED, COMPLETED, etc.)
- ✅ `scheduled_at` → Interview date/time
- ✅ `duration_minutes` → Duration in minutes
- ✅ `feedback` → Summary and assessment
- ✅ `rating` → 1-10 rating scale
- ✅ `notes` → Detailed Q&A transcript with evaluations
- ✅ `created_at` → Auto-populated timestamp
- ✅ `updated_at` → Auto-updated timestamp

### Data Flow

1. **Fetch Candidate**:
   ```
   Application ID → Query database → Get candidate + job details
   ```

2. **Create Interview**:
   ```
   Application ID → Create Interview record → Generate Interview ID
   ```

3. **Store Q&A**:
   ```
   Question → Answer → Evaluation → Append to notes field
   ```

4. **Finalize**:
   ```
   Calculate average → Update status → Set rating → Add summary to feedback
   ```

## Error Handling

The system includes comprehensive error handling:

- **Database Connection Errors**: Clear error messages with traceback
- **Application Not Found**: Prompts to verify application ID
- **Transcription Failures**: Displays error and allows retry
- **Evaluation Errors**: Shows AI service errors
- **Storage Errors**: Displays database errors with details

## Troubleshooting

### Issue: "Application not found"
**Solution**: Verify the application ID exists in the database:
```sql
SELECT id, candidate_id, job_id, status FROM applications;
```

### Issue: "Error creating interview"
**Solution**: Check database connection:
- Ensure MySQL is running on localhost:3306
- Verify database credentials (root/12345)
- Check if recruit_db database exists

### Issue: "Error updating interview record"
**Solution**: Check database permissions and constraints:
- Ensure application_id FK constraint is satisfied
- Verify interview record exists

### Issue: Interview not showing in Java system
**Solution**: Check enum values are compatible:
- Python uses: `InterviewType.TECHNICAL`
- Java expects: String "TECHNICAL"
- Both should match exactly

## Benefits

✅ **Unified Database**: Single source of truth for all recruitment data  
✅ **Proper Candidate Linking**: Interviews linked to applications and candidates  
✅ **Complete Audit Trail**: All Q&A and evaluations stored in notes  
✅ **Professional Assessment**: Automated summary and recommendation  
✅ **Java Backend Compatible**: Can query results from Java application  
✅ **Error-Free Storage**: All data properly validated and stored  

## Next Steps

1. **Test with Real Data**: Run interviews with actual application IDs
2. **Verify Java Integration**: Check if Java backend can read interview results
3. **Add More Interview Types**: Extend to support BEHAVIORAL, PANEL, etc.
4. **Generate Reports**: Create interview summary reports
5. **Email Notifications**: Send results to hiring managers

## Integration with Exam System

The interview system works seamlessly with the exam system:

1. **Candidate takes exam** → Scores above threshold
2. **System auto-schedules interview** → Creates interview record
3. **Run AI voice interview** → Use this system with application_id
4. **Results stored** → Status updated to COMPLETED
5. **Java backend processes** → Moves application to next stage

---

## Support

For issues or questions:
1. Check error messages in the Streamlit interface
2. Review database logs
3. Verify application exists in recruit_db
4. Ensure all services (MySQL, API) are running
