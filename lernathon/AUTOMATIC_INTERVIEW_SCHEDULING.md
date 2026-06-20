# Automatic Interview Scheduling

## Overview
This feature automatically schedules interviews for candidates who pass their coding exams. When a candidate completes an exam with a score of 60% or higher, the system automatically:

1. Updates the application status to `INTERVIEWING`
2. Creates an interview record in the database
3. Schedules the interview for 2 business days from the exam completion

## Implementation Details

### Backend Changes

#### ExamSubmissionService.java
Located at: `backend/src/main/java/com/lernathon/recruitment/service/ExamSubmissionService.java`

**Changes:**
1. Added `InterviewService` dependency for creating interview records
2. Enhanced `autoProgressToInterview()` method to create interview entities

**Key Logic:**
```java
private void autoProgressToInterview(ExamAttempt exam, double finalScore) {
    if (finalScore >= 60.0 && exam.getApplication() != null) {
        Application application = exam.getApplication();
        
        if (application.getStatus() == SCREENING || application.getStatus() == UNDER_REVIEW) {
            // Update application status
            application.setStatus(INTERVIEWING);
            applicationRepository.save(application);
            
            // Create interview record
            Interview interview = new Interview();
            interview.setType(TECHNICAL);
            interview.setStatus(SCHEDULED);
            interview.setScheduledAt(LocalDateTime.now().plusDays(2));
            interview.setDurationMinutes(60);
            interview.setNotes("Auto-scheduled after passing exam with score " + finalScore + "%");
            
            interviewService.createInterview(application.getId(), interview);
        }
    }
}
```

## Configuration

### Pass Threshold
- **Default:** 60%
- **Location:** `ExamSubmissionService.java` line ~518
- **Customization:** Change the value in `if (finalScore >= 60.0)` condition

### Interview Scheduling

**Default Settings:**
- **Interview Type:** TECHNICAL
- **Interview Status:** SCHEDULED
- **Scheduled Time:** 2 days from exam completion
- **Duration:** 60 minutes (1 hour)

**To Customize:**
Edit the interview creation code in `autoProgressToInterview()` method:

```java
interview.setType(Interview.InterviewType.TECHNICAL);  // Change type
interview.setScheduledAt(LocalDateTime.now().plusDays(2));  // Change scheduling delay
interview.setDurationMinutes(60);  // Change duration
```

### Available Interview Types
- `PHONE_SCREEN` - Initial phone screening
- `VIDEO` - Video interview
- `TECHNICAL` - Technical assessment interview
- `BEHAVIORAL` - Behavioral interview
- `PANEL` - Panel interview with multiple interviewers
- `FINAL` - Final interview round

### Available Interview Statuses
- `SCHEDULED` - Interview scheduled (auto-set)
- `COMPLETED` - Interview completed
- `CANCELLED` - Interview cancelled
- `NO_SHOW` - Candidate didn't show up
- `RESCHEDULED` - Interview rescheduled

## Database

### Interview Table Schema
The interview record is stored in the `interviews` table with the following fields:

```sql
CREATE TABLE interviews (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    application_id BIGINT,
    type VARCHAR(50),
    status VARCHAR(50),
    scheduled_at DATETIME,
    duration_minutes INT,
    location VARCHAR(255),
    meeting_link VARCHAR(500),
    feedback TEXT,
    rating INT,
    notes TEXT,
    interviewer_id BIGINT,
    created_at DATETIME,
    updated_at DATETIME,
    FOREIGN KEY (application_id) REFERENCES applications(id)
);
```

## Workflow

### Successful Exam Completion (Pass)
1. Candidate completes exam
2. System calculates final score
3. If score >= 60%:
   - Application status → `INTERVIEWING`
   - Interview record created
   - Interview scheduled for 2 days later
   - Notes updated: "Auto-scheduled after passing exam with score X%"
4. Candidate can see scheduled interview in dashboard

### Failed Exam (Below Threshold)
1. Candidate completes exam
2. System calculates final score
3. If score < 60%:
   - Application remains in `SCREENING` status
   - Notes updated: "Exam failed with score X% (Minimum: 60%)"
   - No interview scheduled

## Testing the Feature

### Test Steps:
1. Create a candidate account
2. Apply for a job
3. Take the coding exam
4. Score at least 60% on the exam
5. Complete the exam
6. Check the application status (should be `INTERVIEWING`)
7. Navigate to the interviews page
8. Verify that an interview is automatically scheduled

### Expected Results:
- **Application Status:** Changed to `INTERVIEWING`
- **Interview Record:** Created in database
- **Interview Type:** TECHNICAL
- **Interview Status:** SCHEDULED
- **Scheduled Time:** 2 days from now
- **Duration:** 60 minutes
- **Notes:** "Auto-scheduled after passing exam with score X%"

## Monitoring

### Log Messages
The system logs the following information:

**Successful Interview Creation:**
```
Application {id} auto-progressed to INTERVIEWING status. Exam score: {score}%
Interview {id} automatically created for application {appId} - scheduled for {datetime}
```

**Failed Exam:**
```
Application {id} remains in SCREENING - exam score below threshold: {score}%
```

**Error Handling:**
```
Error auto-progressing application to interview: {error}
```

## Notes

### Important Considerations:
1. **Transaction Safety:** The interview creation is wrapped in try-catch to ensure exam completion succeeds even if interview scheduling fails
2. **Status Check:** Only applications in `SCREENING` or `UNDER_REVIEW` status are auto-progressed
3. **Cheating Detection:** The system includes cheating warnings in the application notes
4. **No Duplicate Interviews:** The system only creates one interview per exam completion

### Future Enhancements:
- [ ] Email notification to candidate and recruiter
- [ ] Configurable pass threshold per job
- [ ] Support for multiple interview rounds
- [ ] Integration with Python AI interview system
- [ ] Calendar integration (Google Calendar, Outlook)
- [ ] SMS notifications
- [ ] Interview availability checking

## API Endpoints

### Complete Exam
**Endpoint:** `POST /api/exams/{examAttemptId}/complete`

**Request Body:**
```json
{
  "sessionToken": "string"
}
```

**Response:**
```json
{
  "id": 1,
  "finalScore": 75.5,
  "totalQuestions": 20,
  "correctAnswers": 15,
  "status": "COMPLETED",
  "completedAt": "2024-02-19T16:45:00"
}
```

**Side Effect:** If score >= 60%, automatically creates interview

### Get Application Interviews
**Endpoint:** `GET /api/interviews/application/{applicationId}`

**Response:**
```json
[
  {
    "id": 1,
    "type": "TECHNICAL",
    "status": "SCHEDULED",
    "scheduledAt": "2024-02-21T10:00:00",
    "durationMinutes": 60,
    "notes": "Auto-scheduled after passing exam with score 75.50%"
  }
]
```

## Troubleshooting

### Issue: Interview not created after passing exam
**Possible Causes:**
1. Application status not in `SCREENING` or `UNDER_REVIEW`
2. Exam score below 60%
3. No associated application with exam
4. Database connection error

**Solution:**
- Check application status
- Verify exam score >= 60%
- Check backend logs for errors

### Issue: Multiple interviews created
**Possible Cause:** Exam completed multiple times

**Solution:** The system should prevent this, but check for duplicate exam completions

### Issue: Wrong interview scheduled time
**Solution:** Modify `interview.setScheduledAt()` in `autoProgressToInterview()` method

## Related Files
- [ExamSubmissionService.java](backend/src/main/java/com/lernathon/recruitment/service/ExamSubmissionService.java) - Main logic
- [InterviewService.java](backend/src/main/java/com/lernathon/recruitment/service/InterviewService.java) - Interview CRUD
- [Interview.java](backend/src/main/java/com/lernathon/recruitment/entity/Interview.java) - Interview entity
- [Application.java](backend/src/main/java/com/lernathon/recruitment/entity/Application.java) - Application entity
- [ExamAttempt.java](backend/src/main/java/com/lernathon/recruitment/entity/ExamAttempt.java) - Exam attempt entity
